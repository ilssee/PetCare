import { Enviador } from '../recordatorio/recordatorio.enviador';
import { agruparAvisos } from './recomendacion.avisos';
import * as repositorio from './recomendacion.repository';

export interface ResumenRecomendaciones {
  reglasNuevas: number;
  mascotas: number; // mascotas a las que les correspondía el aviso
  enviados: number;
  sinCelular: number; // había coincidencia, pero el usuario no tiene celular registrado
  fallidos: number;
}

/**
 * RF26: avisa a los usuarios cuando se incorporan al repositorio recomendaciones nuevas para la especie
 * (y sexo, si la regla lo exige) de alguna de sus mascotas activas. Un aviso por mascota, con todas sus
 * recomendaciones nuevas. Una regla se considera avisada cuando se completa su fecha de notificación,
 * así no se repite en evaluaciones posteriores. Si el usuario no tiene celulares registrados (por ejemplo,
 * desactivó las notificaciones), la coincidencia se da por evaluada pero no se envía nada.
 * Si un envío falla, la regla sigue como nueva y se reintenta en la próxima ejecución; en ese reintento
 * podrían repetirse los avisos de las mascotas que ya los habían recibido.
 */
export async function notificarNuevasRecomendaciones(hoy: string, enviador: Enviador): Promise<ResumenRecomendaciones> {
  const reglasNuevas = await repositorio.reglasSinNotificar();
  const avisos = agruparAvisos(await repositorio.coincidencias(reglasNuevas));
  const resumen: ResumenRecomendaciones = {
    reglasNuevas: reglasNuevas.length,
    mascotas: avisos.length,
    enviados: 0,
    sinCelular: 0,
    fallidos: 0,
  };

  for (const { mascotaId, aviso } of avisos) {
    try {
      if (await enviador.enviar(aviso)) resumen.enviados++;
      else resumen.sinCelular++;
    } catch (error) {
      console.error(`No se pudo avisar de nuevas recomendaciones (mascota ${mascotaId}):`, error);
      resumen.fallidos++;
    }
  }

  if (resumen.fallidos === 0) await repositorio.marcarNotificadas(reglasNuevas, hoy);
  return resumen;
}
