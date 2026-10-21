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
    expiraSegundos: Number(process.env.JWT_EXPIRA_SEGUNDOS ?? 604800), // 7 días
  },
};

if (!env.jwt.secreto) {
  throw new Error('Falta JWT_SECRET en el archivo .env');
}
