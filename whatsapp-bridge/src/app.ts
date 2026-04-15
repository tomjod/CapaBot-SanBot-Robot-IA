import express from 'express';
import cors from 'cors';
import { env } from './config/env';
import { healthRouter } from './routes/health';
import { sessionRouter } from './routes/session';
import { messagesRouter } from './routes/messages';

export const app = express();

app.use(cors());
app.use(express.json());

app.use((req, res, next) => {
  if (req.path === '/health') {
    return next();
  }

  if (!env.INTERNAL_API_KEY) {
    return next();
  }

  const apiKey = req.header('x-api-key');

  if (apiKey !== env.INTERNAL_API_KEY) {
    return res.status(401).json({
      ok: false,
      message: 'Unauthorized',
    });
  }

  return next();
});

app.use(healthRouter);
app.use(sessionRouter);
app.use(messagesRouter);

app.use((err: unknown, _req: express.Request, res: express.Response, _next: express.NextFunction) => {
  const message = err instanceof Error ? err.message : 'Internal server error';

  console.error(err);

  res.status(500).json({
    ok: false,
    message,
  });
});