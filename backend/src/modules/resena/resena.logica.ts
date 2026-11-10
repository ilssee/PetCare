import { HttpError } from '../../common/errors/http-error';
import { DatosResena } from './resena.types';

/** Reglas de las reseñas sin acceso a la base de datos, para poder probarlas por separado. */

export const COMENTARIO_MAXIMO = 500;

export const MENSAJE_SIN_CALIFICACION = 'Selecciona una calificación para enviar tu reseña';

/**
 * RF45: la calificación es obligatoria y va de 1 a 5 estrellas (un número entero); el comentario es opcional.
 * Un comentario vacío o de solo espacios se guarda como si no se hubiera escrito.
 */
export function validarResena(datos: Record<string, unknown>): DatosResena {
  const calificacion = Number(datos.calificacion);
  if (datos.calificacion === undefined || datos.calificacion === null || datos.calificacion === '') {
    throw new HttpError(400, MENSAJE_SIN_CALIFICACION);
  }
  if (!Number.isInteger(calificacion) || calificacion < 1 || calificacion > 5) {
    throw new HttpError(400, 'La calificación debe ser un número de 1 a 5');
  }

  const texto = datos.comentario === undefined || datos.comentario === null ? '' : String(datos.comentario).trim();
  if (texto.length > COMENTARIO_MAXIMO) {
    throw new HttpError(400, `El comentario no puede pasar de ${COMENTARIO_MAXIMO} caracteres`);
  }
  return { calificacion, comentario: texto === '' ? null : texto };
}

/** Promedio de calificaciones con un decimal (4.67 se muestra como 4.7), o null si no hay reseñas. */
export function promedioRedondeado(suma: number, total: number): number | null {
  return total === 0 ? null : Math.round((suma / total) * 10) / 10;
}
