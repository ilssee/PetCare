import mysql from 'mysql2/promise';
import { env } from './env';

/** Conexión compartida a MySQL. Los repositorios de cada módulo la usan para sus consultas SQL. */
export const pool = mysql.createPool({
  host: env.db.host,
  port: env.db.port,
  user: env.db.user,
  password: env.db.password,
  database: env.db.name,
  waitForConnections: true,
  connectionLimit: 10,
  // Las fechas (DATE) se devuelven como texto "AAAA-MM-DD" y los decimales como números.
  dateStrings: ['DATE'],
  decimalNumbers: true,
});
