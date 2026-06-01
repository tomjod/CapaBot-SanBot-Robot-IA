import { Router } from 'express';
import { whatsappService } from '../services/whatsapp.service';

export const healthRouter = Router();

healthRouter.get('/health', (_req, res) => {
  res.json({
    ok: true,
    service: 'whatsapp-bridge',
    whatsapp: whatsappService.getStatus(),
  });
});