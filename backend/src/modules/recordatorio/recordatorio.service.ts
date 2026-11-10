import { HttpError } from '../../common/errors/http-error';
import { decidirAvisos } from './recordatorio.avisos';
import { DIAS, Dia } from './recordatorio.dias';
import { Enviador } from './recordatorio.enviador';
import * as repositorio from './recordatorio.repository';

/** Capa de lógica de negocio del módulo recordatorio (RF20, RF21 y RF22). */

/** Cuánto pasó con los avisos de un día: cuántos había, cuántos se enviaron y cuántos no. */
export interface ResumenEnvio {
  calculados: number;
  enviados: number;
  fallidos: number; // no se entregaron: el usuario no tiene celular registrado o el envío falló
}

/**
 * RF20: los días en que el usuario quiere recibir el aviso anticipado.
 * Si no configuró ninguno, se usan todos los días de la semana.
 */
export async function obtenerConfiguracion(usuarioId: number): Promise<{ dias: Dia[] }> {
  const dias = await repositorio.obtenerDias(usuarioId);
  return { dias: dias.length === 0 ? [...DIAS] : dias };
}

/** RF20: guarda los días elegidos. Si no se elige ninguno, se restablecen todos los días. */
export async function guardarConfiguracion(
  usuarioId: number,
  entrada: Record<string, unknown>,
): Promise<{ mensaje: string; dias: Dia[] }> {
  const recibidos = entrada.dias ?? [];
  if (!Array.isArray(recibidos) || recibidos.some((d) => !DIAS.includes(d as Dia))) {
    throw new HttpError(400, 'Los días de recordatorio no son válidos');
  }

  const elegidos = DIAS.filter((dia) => recibidos.includes(dia)); // sin repetidos y de lunes a domingo
  const dias = elegidos.length === 0 ? [...DIAS] : elegidos;
  await repositorio.guardarDias(usuarioId, dias);
  return { mensaje: 'Preferencias de recordatorio actualizadas', dias };
}

/**
 * RF21 y RF22: calcula los avisos de [hoy] y los envía con [enviador]. El cuidado pendiente ya está
 * registrado en el cronograma (estado y fecha recurrente), así que no se guarda nada más: si el usuario
 * no puede recibir la notificación, el cuidado sigue pendiente y se avisará de nuevo al día siguiente.
 */
export async function enviarAvisosDelDia(hoy: string, enviador: Enviador): Promise<ResumenEnvio> {
  const cuidados = await repositorio.cuidadosParaAvisar(hoy);
  const usuarios = [...new Set(cuidados.map((c) => c.usuarioId))];
  const avisos = decidirAvisos(hoy, cuidados, await repositorio.diasDeUsuarios(usuarios));

  const resumen: ResumenEnvio = { calculados: avisos.length, enviados: 0, fallidos: 0 };
  for (const aviso of avisos) {
    let entregado = false;
    try {
      entregado = await enviador.enviar(aviso);
    } catch (error) {
      console.error('No se pudo enviar el recordatorio:', error);
    }
    if (entregado) resumen.enviados++;
    else resumen.fallidos++;
  }
  return resumen;
}
