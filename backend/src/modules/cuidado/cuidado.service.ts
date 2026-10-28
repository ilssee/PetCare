import { HttpError } from '../../common/errors/http-error';
import { esFechaReal, fechaDeHoy } from '../../common/fechas';
import { textoOpcional } from '../../common/texto';
import * as mascotas from '../mascota/mascota.service';
import * as reglas from '../regla/regla.service';
import * as repositorio from './cuidado.repository';
import {
  ConfiguracionCuidado,
  ConteoCuidados,
  CuidadoDetalle,
  CuidadoRespuesta,
  FiltroEstado,
  ModoTermino,
  NuevoCuidado,
  UnidadFrecuencia,
} from './cuidado.types';

/** Capa de lógica de negocio del módulo cuidado. Los mensajes siguen el ERS (RF11 a RF15 y RF23). */

const MODOS: ModoTermino[] = ['una_vez', 'varias_veces', 'hasta_fecha', 'indefinido'];
const UNIDADES: UnidadFrecuencia[] = ['dias', 'semanas', 'meses', 'anios'];
const FILTROS: FiltroEstado[] = ['todos', 'pendientes', 'finalizados'];

const CAMPO_OBLIGATORIO = 'Este campo es obligatorio';
const MAXIMO_NOMBRE = 150;
const MAXIMO_TEXTO = 1000;
const MENSAJE_INACTIVA = 'La mascota está inactiva. Reactívala para agregar cuidados';

function estaVacio(valor: unknown): boolean {
  return String(valor ?? '').trim() === '';
}

function enteroEntre(valor: unknown, minimo: number, maximo: number): number | null {
  const numero = Number(valor);
  return Number.isInteger(numero) && numero >= minimo && numero <= maximo ? numero : null;
}

interface OpcionesConfiguracion {
  /** Al editar, la fecha de inicio puede ser pasada (por ejemplo, un cuidado que ya venció). */
  permitirFechaPasada?: boolean;
}

/**
 * Valida cómo se programa un cuidado (modo de término, frecuencia, fechas, aviso y nota).
 * Es la misma configuración para los cuidados recomendados (RF11), los personalizados (RF12)
 * y al editar un cuidado (RF14).
 */
export function validarConfiguracion(
  entrada: Record<string, unknown>,
  opciones: OpcionesConfiguracion = {},
): ConfiguracionCuidado {
  // Modo de término
  if (estaVacio(entrada.modoTermino)) throw new HttpError(400, CAMPO_OBLIGATORIO);
  const modoTermino = String(entrada.modoTermino).trim() as ModoTermino;
  if (!MODOS.includes(modoTermino)) throw new HttpError(400, 'El modo de término no es válido');

  // Fecha de inicio: será la primera (o la próxima) fecha recurrente del cuidado
  if (estaVacio(entrada.fechaInicio)) throw new HttpError(400, CAMPO_OBLIGATORIO);
  const fechaInicio = String(entrada.fechaInicio).trim();
  if (!esFechaReal(fechaInicio)) throw new HttpError(400, 'La fecha de inicio no es válida (formato AAAA-MM-DD)');
  if (!opciones.permitirFechaPasada && fechaInicio < fechaDeHoy()) {
    throw new HttpError(400, 'La fecha de inicio no puede ser una fecha pasada');
  }

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
  if (nota !== null && nota.length > MAXIMO_TEXTO) {
    throw new HttpError(400, `La nota debe tener hasta ${MAXIMO_TEXTO} caracteres`);
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

/** Nombre y descripción de un cuidado personalizado (RF12). La descripción es opcional. */
function validarNombreYDescripcion(entrada: Record<string, unknown>): { nombre: string; descripcion: string } {
  const nombre = String(entrada.nombre ?? '').trim();
  if (nombre === '') throw new HttpError(400, CAMPO_OBLIGATORIO);
  if (nombre.length > MAXIMO_NOMBRE) throw new HttpError(400, `El nombre debe tener hasta ${MAXIMO_NOMBRE} caracteres`);

  const descripcion = String(entrada.descripcion ?? '').trim();
  if (descripcion.length > MAXIMO_TEXTO) {
    throw new HttpError(400, `La descripción debe tener hasta ${MAXIMO_TEXTO} caracteres`);
  }
  return { nombre, descripcion };
}

/** Agrega el historial y el contador de actividades realizadas a cada cuidado. */
async function conHistorial(cuidados: CuidadoRespuesta[]): Promise<CuidadoDetalle[]> {
  const actividades = await repositorio.listarActividades(cuidados.map((c) => c.id));
  return cuidados.map((cuidado) => {
    const propias = actividades.filter((a) => a.cuidadoId === cuidado.id);
    return { ...cuidado, actividades: propias, realizadas: propias.filter((a) => a.estado === 'completada').length };
  });
}

/** Un cuidado de una mascota del usuario. Si no existe o es de otro usuario, responde 404. */
async function obtenerPropio(id: number, usuarioId: number): Promise<CuidadoRespuesta> {
  const cuidado = await repositorio.buscarPropio(id, usuarioId);
  if (!cuidado) throw new HttpError(404, 'Cuidado no encontrado');
  return cuidado;
}

/** RF23: cuidados de una mascota, con su historial, según el estado elegido. */
export async function listar(
  mascotaId: number,
  usuarioId: number,
  estado: unknown,
): Promise<{ conteos: ConteoCuidados; cuidados: CuidadoDetalle[] }> {
  const mascota = await mascotas.obtener(mascotaId, usuarioId);

  const filtro = (estaVacio(estado) ? 'todos' : String(estado).trim()) as FiltroEstado;
  if (!FILTROS.includes(filtro)) throw new HttpError(400, 'El estado debe ser todos, pendientes o finalizados');

  const [conteos, cuidados] = await Promise.all([
    repositorio.contarPorEstado(mascota.id),
    repositorio.listarPorMascota(mascota.id, filtro),
  ]);
  return { conteos, cuidados: await conHistorial(cuidados) };
}

export async function obtener(id: number, usuarioId: number): Promise<CuidadoDetalle> {
  return (await conHistorial([await obtenerPropio(id, usuarioId)]))[0];
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
  if (mascota.estado === 'inactiva') throw new HttpError(409, MENSAJE_INACTIVA);

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

/** RF12: agrega un cuidado personalizado al cronograma de la mascota. */
export async function crearPersonalizado(
  mascotaId: number,
  usuarioId: number,
  entrada: Record<string, unknown>,
): Promise<{ mensaje: string; cuidado: CuidadoDetalle }> {
  const mascota = await mascotas.obtener(mascotaId, usuarioId);
  if (mascota.estado === 'inactiva') throw new HttpError(409, MENSAJE_INACTIVA);

  const { nombre, descripcion } = validarNombreYDescripcion(entrada);
  const nuevo: NuevoCuidado = { reglaId: null, nombre, descripcion, ...validarConfiguracion(entrada) };

  try {
    const [id] = await repositorio.crearVarios(mascota.id, [nuevo]);
    return { mensaje: 'Cuidado agregado exitosamente', cuidado: await obtener(id, usuarioId) };
  } catch (error) {
    console.error('Error al agregar el cuidado:', error);
    throw new HttpError(500, 'No se pudo agregar el cuidado. Intente nuevamente');
  }
}

/**
 * RF14: modifica un cuidado. En un cuidado recomendado el nombre y la descripción están
 * bloqueados (vienen de la regla); en uno personalizado también se pueden cambiar.
 * La fecha ingresada pasa a ser la próxima fecha y la frecuencia rige desde la siguiente actividad.
 */
export async function editar(
  id: number,
  usuarioId: number,
  entrada: Record<string, unknown>,
): Promise<{ mensaje: string; cuidado: CuidadoDetalle }> {
  const actual = await obtenerPropio(id, usuarioId);
  if (actual.estado === 'finalizado') throw new HttpError(409, 'Este cuidado ya finalizó y no se puede editar');

  const esRecomendado = actual.reglaId !== null;
  const { nombre, descripcion } = esRecomendado
    ? { nombre: actual.nombre, descripcion: actual.descripcion }
    : validarNombreYDescripcion(entrada);
  const configuracion = validarConfiguracion(entrada, { permitirFechaPasada: true });

  await repositorio.actualizar(id, {
    nombre,
    descripcion,
    nuevo: { reglaId: actual.reglaId, nombre, descripcion, ...configuracion },
  });
  return { mensaje: 'Cuidado actualizado correctamente', cuidado: await obtener(id, usuarioId) };
}

/** RF15: elimina el cuidado junto con su historial. Si era recomendado, vuelve a las recomendaciones. */
export async function eliminar(id: number, usuarioId: number): Promise<void> {
  await obtenerPropio(id, usuarioId);
  await repositorio.eliminar(id);
}
