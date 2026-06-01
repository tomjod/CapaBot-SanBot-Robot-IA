import dotenv from 'dotenv';

dotenv.config();

function required(name: string, fallback?: string): string {
  const value = process.env[name] ?? fallback;
  if (!value) {
    throw new Error(`Missing environment variable: ${name}`);
  }
  return value;
}

export const env = {
  PORT: Number(process.env.PORT ?? 3001),
  NODE_ENV: process.env.NODE_ENV ?? 'development',
  INTERNAL_API_KEY: required('INTERNAL_API_KEY', ''),
  AUTH_DIR: required('AUTH_DIR', '.auth'),
  RECONNECT_INTERVAL_MS: Number(process.env.RECONNECT_INTERVAL_MS ?? 5000),
};