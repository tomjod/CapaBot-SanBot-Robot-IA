import fs from 'node:fs/promises';
import path from 'node:path';
import makeWASocket, {
  Browsers,
  fetchLatestBaileysVersion,
  type WASocket,
  useMultiFileAuthState,
} from '@whiskeysockets/baileys';
import pino from 'pino';

type CreateClientResult = {
  sock: WASocket;
  saveCreds: () => Promise<void>;
};

export async function createBaileysClient(authDir: string): Promise<CreateClientResult> {
  const resolvedAuthDir = path.resolve(process.cwd(), authDir);
  await fs.mkdir(resolvedAuthDir, { recursive: true });

  const { state, saveCreds } = await useMultiFileAuthState(resolvedAuthDir);

  // Fetch the current WhatsApp Web version instead of pinning a stale one.
  // A pinned/outdated version is a known cause of 405 / connectionFailure
  // right after scanning the QR.
  const { version, isLatest } = await fetchLatestBaileysVersion();
  console.log(`[whatsapp] using WA version ${version.join('.')} (isLatest=${isLatest})`);

  const sock = makeWASocket({
    auth: state,
    logger: pino({ level: 'silent' }),
    markOnlineOnConnect: false,
    version,
    browser: Browsers.macOS('Google Chrome'),
  });

  return { sock, saveCreds };
}