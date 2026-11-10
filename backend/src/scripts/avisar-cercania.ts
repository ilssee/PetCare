import { pool } from '../config/database';
import { ejecutarAvisoDeCercania } from '../jobs/recordatorios.job';

/**
 * Avisa ahora mismo de las campañas y veterinarias nuevas cercanas a los usuarios, sin esperar
 * a la revisión automática de cada 10 minutos.
 * Uso: npm run cercania:ahora
 */
async function main() {
  const resumen = await ejecutarAvisoDeCercania();
  console.log(resumen);
  await pool.end();
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
