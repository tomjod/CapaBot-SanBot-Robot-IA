import { Router } from 'express';
import { whatsappService } from '../services/whatsapp.service';

export const messagesRouter = Router();

messagesRouter.post('/messages/text', async (req, res, next) => {
  try {
    const { phone, text } = req.body as {
      phone?: string;
      text?: string;
    };

    if (!phone || !text) {
      return res.status(400).json({
        ok: false,
        message: 'phone and text are required',
      });
    }

    const result = await whatsappService.sendTextMessage(phone, text);

    return res.json(result);
  } catch (error) {
    next(error);
  }
});