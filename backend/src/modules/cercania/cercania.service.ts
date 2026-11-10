import { HttpError } from '../../common/errors/http-error';
import { Enviador } from '../recordatorio/recordatorio.enviador';
import { avisosDeLugar } from './cercania.logica';
import * as repositorio from './cercania.repository';
import { ConfiguracionCercania } from './cercania.types';

/** Radio máximo que se acepta, en kilómetros. La app ofrece de 1 a 10. */
const RADIO_MAXIMO_KM = 50;

export const MENSAJE_SIN_UBICACION = 'Configura tu ubicación y radio de interés para activar esta notificación';

export async function obtener(usuarioId: number): Promise<ConfiguracionCercania> {
  return repositorio.obtener(usuarioId);
}

function numeroONulo(valor: unknown, mensajeError: string, minimo: number, maximo: number): number | null {
  if (valor === undefined || valor === null || valor === '') return null;
  const n = Number(valor);
  if (!Number.isFinite(n) || n < minimo || n > maximo) throw new HttpError(400, mensajeError);
  return n;
}

function booleano(valor: unknown, nombre: string): boolean {
  if (valor === undefined || valor === null) return false;
  if (typeof valor !== 'boolean') throw new HttpError(400, `${nombre} debe ser verdadero o falso`);
  return valor;
}

/**
 * RF37 y RF38: guarda la ubicación de referencia, el radio y los avisos activados.
 * La ubicación y el radio son opcionales, pero sin ellos no se puede activar ningún aviso.
 */
export async function guardar(usuarioId: number, datos: Record<string, unknown>): Promise<ConfiguracionCercania> {
  const lat = numeroONulo(datos.lat, 'La latitud no es válida', -90, 90);
  const lng = numeroONulo(datos.lng, 'La longitud no es válida', -180, 180);
  if ((lat === null) !== (lng === null)) throw new HttpError(400, 'Envía la latitud y la longitud juntas');
  const radioKm = numeroONulo(datos.radioKm, 'El radio no es válido', 0.1, RADIO_MAXIMO_KM);

  const notificarCampanas = booleano(datos.notificarCampanas, 'El aviso de campañas');
  const notificarVeterinarias = booleano(datos.notificarVeterinarias, 'El aviso de veterinarias');
  const configurado = lat !== null && lng !== null && radioKm !== null;
  if ((notificarCampanas || notificarVeterinarias) && !configurado) {
    throw new HttpError(400, MENSAJE_SIN_UBICACION);
  }

  const configuracion = { lat, lng, radioKm, notificarCampanas, notificarVeterinarias };
  await repositorio.guardar(usuarioId, configuracion);
  return configuracion;
}

export interface ResumenCercania {
  campanasNuevas: number;
  veterinariasNuevas: number;
  enviados: number;
  sinCelular: number; // había coincidencia, pero el usuario no tiene celular registrado
  fallidos: number;
}

/**
 * RF39 y RF40: avisa a los usuarios cuando hay una campaña o una veterinaria nueva dentro de su radio.
 * Un lugar es nuevo mientras su fecha de notificación está vacía; al terminar se completa para no volver
 * a avisarlo. Si el usuario no tiene celulares registrados (por ejemplo, desactivó las notificaciones),
 * la coincidencia se da por evaluada pero no se envía nada. Si un envío falla, los lugares quedan como
 * nuevos y se reintentan en la próxima ejecución; en ese reintento podrían repetirse avisos ya entregados.
 */
export async function avisarLugaresNuevos(hoy: string, enviador: Enviador): Promise<ResumenCercania> {
  const [campanas, veterinarias, usuarios] = await Promise.all([
    repositorio.campanasSinAvisar(hoy),
    repositorio.veterinariasSinAvisar(),
    repositorio.usuariosConAvisos(),
  ]);
  const resumen: ResumenCercania = {
    campanasNuevas: campanas.length,
    veterinariasNuevas: veterinarias.length,
    enviados: 0,
    sinCelular: 0,
    fallidos: 0,
  };

  for (const lugar of [...campanas, ...veterinarias]) {
    for (const aviso of avisosDeLugar(lugar, usuarios)) {
      try {
        if (await enviador.enviar(aviso)) resumen.enviados++;
        else resumen.sinCelular++;
      } catch (error) {
        console.error(`No se pudo avisar del lugar ${lugar.tipo} ${lugar.id} (usuario ${aviso.usuarioId}):`, error);
        resumen.fallidos++;
      }
    }
  }

  if (resumen.fallidos === 0) {
    await repositorio.marcarCampanasAvisadas(campanas.map((c) => c.id), hoy);
    await repositorio.marcarVeterinariasAvisadas(veterinarias.map((v) => v.id), hoy);
  }
  return resumen;
}
