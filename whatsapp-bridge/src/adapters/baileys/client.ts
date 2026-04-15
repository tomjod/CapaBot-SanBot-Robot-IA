import fs from 'node:fs/promises';
import path from 'node:path';
import makeWASocket, {
  Browsers,
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

  const sock = makeWASocket({
    auth: state,
    logger: pino({ level: 'silent' }),
    markOnlineOnConnect: false,

    // workaround para el 405 reportado recientemente
    version: [2, 3000, 1034074495],

    // aunque la doc lo remarca sobre todo para pairing code,
    // hoy vale la pena probar un browser lógico tipo macOS
    browser: Browsers.macOS('Google Chrome'),
  });

  return { sock, saveCreds };
}