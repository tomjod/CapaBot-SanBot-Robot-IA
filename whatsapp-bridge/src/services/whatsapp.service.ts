import QRCode from 'qrcode';
import type { WASocket } from '@whiskeysockets/baileys';
import { DisconnectReason } from '@whiskeysockets/baileys';
import { Boom } from '@hapi/boom';
import { createBaileysClient } from '../adapters/baileys/client';
import { env } from '../config/env';

type BridgeStatus = 'idle' | 'connecting' | 'open' | 'closed';

class WhatsAppService {
  private sock: WASocket | null = null
  private isStarting = false
  private status: BridgeStatus = 'idle';
  private lastQr: string | null = null;
  private lastError: string | null = null;
  private startPromise: Promise<void> | null = null;
  private reconnectTimer: NodeJS.Timeout | null = null;

  async start(): Promise<void> {
    if (this.isStarting) return
    this.isStarting = true

    if (this.sock && (this.status === 'connecting' || this.status === 'open')) {
      return;
    }

    if (this.startPromise) {
      return this.startPromise;
    }

    this.startPromise = this.bootstrap().finally(() => {
      this.startPromise = null;
    });

    return this.startPromise;
  }

  private async bootstrap(): Promise<void> {
    this.status = 'connecting';
    this.lastError = null;

    const { sock, saveCreds } = await createBaileysClient(env.AUTH_DIR);
    this.sock = sock;

    sock.ev.on('creds.update', saveCreds);

    sock.ev.on('connection.update', async (update) => {
    const { connection, lastDisconnect, qr } = update;

    console.log('[connection.update]', JSON.stringify({
      connection,
      hasQr: Boolean(qr),
      statusCode: (lastDisconnect?.error as Boom | undefined)?.output?.statusCode,
      errorMessage: lastDisconnect?.error instanceof Error
        ? lastDisconnect.error.message
        : null,
    }, null, 2));

    if (qr) {
      console.log('[whatsapp] QR received');
      console.log(await QRCode.toString(qr, { type: 'terminal', small: true }));
    }

    if (connection === 'open') {
      console.log('[whatsapp] connected');
      this.status = 'open';
    }

    if (connection === 'close') {
      const statusCode = (lastDisconnect?.error as Boom | undefined)?.output?.statusCode;

      if (statusCode === DisconnectReason.restartRequired) {
        console.log('[whatsapp] restart required, creating new socket...')
        this.sock = null
        this.isStarting = false
        setTimeout(() => void this.start(), 1000)
        return
      }

      if (statusCode === DisconnectReason.loggedOut) {
        console.log('[whatsapp] logged out, delete auth and scan again');
        this.sock = null
        this.isStarting = false
        return;
      }

      if (statusCode === 401) {
        console.error('[whatsapp] conflict (401) - another device is using this session. Close WhatsApp on your phone and try again.');
        return;
      }

      console.log(`[whatsapp] closed with statusCode=${statusCode}, reconnecting...`);
      setTimeout(() => void this.start(), 3000);
    }
      });
  }

  private scheduleReconnect(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
    }

    this.reconnectTimer = setTimeout(() => {
      void this.start();
    }, env.RECONNECT_INTERVAL_MS);
  }

  getStatus() {
    return {
      status: this.status,
      isConnected: this.status === 'open',
      hasQr: Boolean(this.lastQr),
      lastError: this.lastError,
    };
  }

  getQr() {
    return this.lastQr;
  }

  async sendTextMessage(phone: string, text: string) {
    if (!this.sock || this.status !== 'open') {
      throw new Error('WhatsApp is not connected yet');
    }

    const jid = this.toJid(phone);

    const result = await this.sock.sendMessage(jid, { text });

    return {
      ok: true,
      jid,
      messageId: result?.key?.id ?? null,
    };
  }

  private toJid(phone: string): string {
    if (phone.endsWith('@s.whatsapp.net') || phone.endsWith('@g.us')) {
      return phone;
    }

    const digits = phone.replace(/\D/g, '');

    if (!digits || digits.length < 8) {
      throw new Error('Invalid phone number');
    }

    return `${digits}@s.whatsapp.net`;
  }
}

export const whatsappService = new WhatsAppService();