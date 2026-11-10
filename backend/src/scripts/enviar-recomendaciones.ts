import { pool } from '../config/database';
import { ejecutarAvisoDeRecomendaciones } from '../jobs/recordatorios.job';

/**
 * Avisa ahora mismo de las recomendaciones nuevas del repositorio, sin esperar a las 8:00 a. m.
 * Uso: npm run recomendaciones:ahora
 */
async function main() {
  await ejecutarAvisoDeRecomendaciones();
  await pool.end();
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
