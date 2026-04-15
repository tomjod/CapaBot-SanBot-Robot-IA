import { Router } from 'express';
import { whatsappService } from '../services/whatsapp.service';

export const sessionRouter = Router();

sessionRouter.post('/session/start', async (_req, res, next) => {
  try {
    await whatsappService.start();

    res.json({
      ok: true,
      message: 'WhatsApp bootstrap started',
      whatsapp: whatsappService.getStatus(),
    });
  } catch (error) {
    next(error);
  }
});

sessionRouter.get('/session/status', (_req, res) => {
  res.json({
    ok: true,
    whatsapp: whatsappService.getStatus(),
  });
});

sessionRouter.get('/session/qr', (_req, res) => {
  const qr = whatsappService.getQr();

  if (!qr) {
    return res.status(404).json({
      ok: false,
      message: 'QR not available right now',
    });
  }

  return res.json({
    ok: true,
    qr,
  });
});