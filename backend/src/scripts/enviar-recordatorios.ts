import { pool } from '../config/database';
import { ejecutarRecordatoriosDeHoy } from '../jobs/recordatorios.job';

/**
 * Envía ahora mismo los recordatorios de hoy, sin esperar a las 8:00 a. m.
 * Uso: npm run recordatorios:ahora
 * Cada aviso se envía una sola vez por día: si ya se enviaron, aparecen como "ya enviados antes".
 */
async function main() {
  await ejecutarRecordatoriosDeHoy();
  await pool.end();
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
