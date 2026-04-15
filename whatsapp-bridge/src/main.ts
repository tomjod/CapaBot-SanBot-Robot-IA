import { app } from './app';
import { env } from './config/env';
import { whatsappService } from './services/whatsapp.service';

async function bootstrap() {
  if (!app) {
    throw new Error('app is undefined - import failed');
  }
  
  app.listen(env.PORT, () => {
    console.log(`[server] whatsapp-bridge listening on port ${env.PORT}`);
  });

  try {
    await whatsappService.start();
  } catch (error) {
    console.error('[startup] failed to initialize whatsapp service', error);
  }
}

void bootstrap();