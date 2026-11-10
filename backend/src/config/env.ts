import dotenv from 'dotenv';

dotenv.config();

/** Variables de entorno de la aplicación, leídas una sola vez desde el archivo .env. */
export const env = {
  port: Number(process.env.PORT ?? 3000),
  db: {
    host: process.env.DB_HOST ?? '127.0.0.1',
    port: Number(process.env.DB_PORT ?? 3306),
    user: process.env.DB_USER ?? '',
    password: process.env.DB_PASSWORD ?? '',
    name: process.env.DB_NAME ?? 'petcare',
  },
  jwt: {
    secreto: process.env.JWT_SECRET ?? '',
  },
  firebase: {
    // Clave de cuenta de servicio de Firebase (archivo secreto; no se sube a GitHub).
    clave: process.env.FIREBASE_CLAVE ?? './firebase-clave.json',
  },
  recordatorios: {
    // Pon RECORDATORIOS_ACTIVOS=false para que el servidor no programe los avisos de las 8:00 a. m.
    activos: process.env.RECORDATORIOS_ACTIVOS !== 'false',
  },
};

if (!env.jwt.secreto) {
  throw new Error('Falta JWT_SECRET en el archivo .env');
}
