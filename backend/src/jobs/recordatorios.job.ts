import * as cron from 'node-cron';
import { fechaDeHoy } from '../common/fechas';
import { mensajeria } from '../config/firebase';
import { Enviador, EnviadorEnConsola } from '../modules/recordatorio/recordatorio.enviador';
import { EnviadorFirebase } from '../modules/recordatorio/recordatorio.firebase';
import { notificarNuevasRecomendaciones, ResumenRecomendaciones } from '../modules/recomendacion/recomendacion.service';
import { enviarAvisosDelDia, ResumenEnvio } from '../modules/recordatorio/recordatorio.service';

/**
 * Proceso diario de notificaciones. Todos los días a las 8:00 a. m. (hora de Lima) envía como push
 * los recordatorios de cuidados (RF21 y RF22) y el aviso de nuevas recomendaciones (RF26).
 */

/** Firebase si hay clave; si no, los avisos solo se muestran en la consola del servidor. */
export function crearEnviador(): Enviador {
  const conexion = mensajeria();
  return conexion ? new EnviadorFirebase(conexion) : new EnviadorEnConsola();
}

/** Calcula y envía los avisos de hoy. También se puede lanzar a mano con `npm run recordatorios:ahora`. */
export async function ejecutarRecordatoriosDeHoy(enviador: Enviador = crearEnviador()): Promise<ResumenEnvio> {
  const hoy = fechaDeHoy();
  const resumen = await enviarAvisosDelDia(hoy, enviador);
  console.log(
    `[recordatorios ${hoy}] por avisar: ${resumen.calculados} · enviados: ${resumen.enviados} · ` +
      `sin entregar: ${resumen.fallidos}`,
  );
  return resumen;
}

/** RF26. También se puede lanzar a mano con `npm run recomendaciones:ahora`. */
export async function ejecutarAvisoDeRecomendaciones(enviador: Enviador = crearEnviador()): Promise<ResumenRecomendaciones> {
  const hoy = fechaDeHoy();
  const resumen = await notificarNuevasRecomendaciones(hoy, enviador);
  console.log(
    `[recomendaciones ${hoy}] reglas nuevas: ${resumen.reglasNuevas} · mascotas por avisar: ${resumen.mascotas} · ` +
      `enviados: ${resumen.enviados} · sin celular: ${resumen.sinCelular} · sin entregar: ${resumen.fallidos}`,
  );
  return resumen;
}

export function programarRecordatorios(): void {
  cron.schedule(
    '0 8 * * *', // minuto 0 de la hora 8, todos los días
    () => {
      ejecutarRecordatoriosDeHoy()
        .then(() => ejecutarAvisoDeRecomendaciones())
        .catch((error) => console.error('Falló el proceso de notificaciones:', error));
    },
    { timezone: 'America/Lima' },
  );
  console.log('Notificaciones programadas: todos los días a las 8:00 a. m. (hora de Lima)');
}
