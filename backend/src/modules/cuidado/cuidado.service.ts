import { HttpError } from '../../common/errors/http-error';
import { esFechaReal, fechaDeHoy } from '../../common/fechas';
import { textoOpcional } from '../../common/texto';
import * as mascotas from '../mascota/mascota.service';
import * as reglas from '../regla/regla.service';
import * as repositorio from './cuidado.repository';
import {
  ConfiguracionCuidado,
  CuidadoRespuesta,
  ModoTermino,
  NuevoCuidado,
  UnidadFrecuencia,
} from './cuidado.types';

/** Capa de lógica de negocio del módulo cuidado. Los mensajes siguen el ERS (RF11 y RF12). */

const MODOS: ModoTermino[] = ['una_vez', 'varias_veces', 'hasta_fecha', 'indefinido'];
const UNIDADES: UnidadFrecuencia[] = ['dias', 'semanas', 'meses', 'anios'];

const CAMPO_OBLIGATORIO = 'Este campo es obligatorio';
const MAXIMO_NOTA = 1000;

function estaVacio(valor: unknown): boolean {
  return String(valor ?? '').trim() === '';
}

function enteroEntre(valor: unknown, minimo: number, maximo: number): number | null {
  const numero = Number(valor);
  return Number.isInteger(numero) && numero >= minimo && numero <= maximo ? numero : null;
}

/**
 * Valida cómo se programa un cuidado (modo de término, frecuencia, fechas, aviso y nota).
 * Es la misma configuración para los cuidados recomendados (RF11) y los personalizados (RF12).
 */
export function validarConfiguracion(entrada: Record<string, unknown>): ConfiguracionCuidado {
  // Modo de término
  if (estaVacio(entrada.modoTermino)) throw new HttpError(400, CAMPO_OBLIGATORIO);
  const modoTermino = String(entrada.modoTermino).trim() as ModoTermino;
  if (!MODOS.includes(modoTermino)) throw new HttpError(400, 'El modo de término no es válido');

  // Fecha de inicio: será la primera fecha recurrente del cuidado
  if (estaVacio(entrada.fechaInicio)) throw new HttpError(400, CAMPO_OBLIGATORIO);
  const fechaInicio = String(entrada.fechaInicio).trim();
  if (!esFechaReal(fechaInicio)) throw new HttpError(400, 'La fecha de inicio no es válida (formato AAAA-MM-DD)');
  if (fechaInicio < fechaDeHoy()) throw new HttpError(400, 'La fecha de inicio no puede ser una fecha pasada');

  // Frecuencia: no aplica cuando el cuidado es de una sola vez
  let frecuenciaValor: number | null = null;
  let frecuenciaUnidad: UnidadFrecuencia | null = null;
  if (modoTermino !== 'una_vez') {
    if (estaVacio(entrada.frecuenciaValor) || estaVacio(entrada.frecuenciaUnidad)) {
      throw new HttpError(400, CAMPO_OBLIGATORIO);
    }
    frecuenciaValor = enteroEntre(entrada.frecuenciaValor, 1, 999);
    if (frecuenciaValor === null) throw new HttpError(400, 'La frecuencia debe ser un número entero entre 1 y 999');
    frecuenciaUnidad = String(entrada.frecuenciaUnidad).trim() as UnidadFrecuencia;
    if (!UNIDADES.includes(frecuenciaUnidad)) throw new HttpError(400, 'La unidad de la frecuencia no es válida');
  }

  // Número de veces: solo en "varias veces"
  let totalRepeticiones: number | null = null;
  if (modoTermino === 'varias_veces') {
    if (estaVacio(entrada.totalRepeticiones)) throw new HttpError(400, CAMPO_OBLIGATORIO);
    totalRepeticiones = enteroEntre(entrada.totalRepeticiones, 1, 999);
    if (totalRepeticiones === null) throw new HttpError(400, 'El número de veces debe ser un entero entre 1 y 999');
  }

  // Fecha límite: solo en "hasta una fecha específica"
  let fechaLimite: string | null = null;
  if (modoTermino === 'hasta_fecha') {
    if (estaVacio(entrada.fechaLimite)) throw new HttpError(400, CAMPO_OBLIGATORIO);
    fechaLimite = String(entrada.fechaLimite).trim();
    if (!esFechaReal(fechaLimite)) throw new HttpError(400, 'La fecha límite no es válida (formato AAAA-MM-DD)');
    if (fechaLimite <= fechaInicio) throw new HttpError(400, 'La fecha límite debe ser posterior a la fecha de inicio');
  }

  const nota = textoOpcional(entrada.nota);
  if (nota !== null && nota.length > MAXIMO_NOTA) {
    throw new HttpError(400, `La nota debe tener hasta ${MAXIMO_NOTA} caracteres`);
  }

  return {
    modoTermino,
    frecuenciaValor,
    frecuenciaUnidad,
    totalRepeticiones,
    fechaLimite,
    fechaInicio,
    avisoAnticipado: entrada.avisoAnticipado === true,
    nota,
  };
}

/**
 * RF11: confirma la propuesta. Cada cuidado seleccionado llega con su configuración
 * (los que el usuario omite simplemente no vienen) y se incorpora al cronograma activo.
 */
export async function activarRecomendados(
  mascotaId: number,
  usuarioId: number,
  entrada: Record<string, unknown>,
): Promise<{ mensaje: string; cuidados: CuidadoRespuesta[] }> {
  const mascota = await mascotas.obtener(mascotaId, usuarioId);
  if (mascota.estado === 'inactiva') {
    throw new HttpError(409, 'La mascota está inactiva. Reactívala para agregar cuidados');
  }

  const seleccion = entrada.cuidados;
  if (!Array.isArray(seleccion) || seleccion.length === 0) {
    throw new HttpError(400, 'Seleccione al menos un cuidado');
  }

  // Solo se aceptan reglas que aplican a esta mascota y que aún no están en su cronograma.
  const disponibles = new Map((await reglas.listarDisponibles(mascota.id, usuarioId)).map((r) => [r.id, r]));
  const vistas = new Set<number>();

  // Se valida todo antes de guardar: si un cuidado está mal, no se guarda ninguno.
  const nuevos: NuevoCuidado[] = seleccion.map((item) => {
    const datos = (item ?? {}) as Record<string, unknown>;
    const reglaId = Number(datos.reglaId);
    const regla = disponibles.get(reglaId);
    if (!regla || vistas.has(reglaId)) {
      throw new HttpError(400, 'Uno de los cuidados seleccionados no está disponible para esta mascota');
    }
    vistas.add(reglaId);
    return { reglaId, nombre: regla.nombre, descripcion: regla.descripcion, ...validarConfiguracion(datos) };
  });

  try {
    const ids = await repositorio.crearVarios(mascota.id, nuevos);
    return { mensaje: 'Cronograma activado correctamente', cuidados: await repositorio.listarPorIds(ids) };
  } catch (error) {
    console.error('Error al activar el cronograma:', error);
    throw new HttpError(500, 'No se pudo activar el cronograma. Intente nuevamente');
  }
}
