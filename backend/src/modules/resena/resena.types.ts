/** Tipos del módulo reseña (RF44 a RF46): opiniones de los usuarios sobre una veterinaria o un organizador. */

export type OrdenResenas = 'recientes' | 'calificadas';

/** Una reseña tal como la ve la app. */
export interface ResenaVista {
  id: number;
  autor: string; // nombre completo con el que el autor se registró
  calificacion: number; // de 1 a 5
  comentario: string | null;
  fecha: string; // AAAA-MM-DD
  esMia: boolean; // la escribió el usuario que consulta: solo esas se pueden editar o eliminar
}

/** El listado de reseñas de un organizador, con su resumen. */
export interface ListadoResenas {
  promedio: number | null; // null si todavía no tiene reseñas
  total: number;
  resenas: ResenaVista[];
}

/** Lo que el usuario envía al escribir o editar una reseña, ya validado. */
export interface DatosResena {
  calificacion: number;
  comentario: string | null;
}
