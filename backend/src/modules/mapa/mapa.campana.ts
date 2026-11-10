import { RowDataPacket } from 'mysql2';
import { HttpError } from '../../common/errors/http-error';
import { pool } from '../../config/database';
import { ahoraEnLima, vistaDeCampana } from './mapa.logica';
import * as repositorio from './mapa.repository';
import { Ahora, CampanaMapa, CampanaRegistro, Punto, VeterinariaRegistro } from './mapa.types';

/**
 * Detalle de una campaña (RF43): todo lo que muestra su pantalla. Si la organiza una entidad externa, incluye el
 * resumen de su organizador (nombre, calificación promedio y total de reseñas) para poder abrir sus reseñas.
 */

export const MENSAJE_CAMPANA_NO_DISPONIBLE = 'No es posible visualizar el detalle de la campaña';

export interface ResumenOrganizador {
  id: number;
  nombre: string;
  promedio: number | null;
  totalResenas: number;
}

export interface DetalleCampana extends CampanaMapa {
  descripcion: string;
  direccion: string | null;
  /** Resumen de quien organiza la campaña, con su id para abrir sus reseñas. */
  organizadorDetalle: ResumenOrganizador;
}

/** Junta la vista de la campaña con su descripción, su dirección y el resumen de su organizador. */
export function armarDetalleDeCampana(
  campana: CampanaRegistro,
  veterinarias: VeterinariaRegistro[],
  ahora: Ahora,
  origen: Punto | null,
  direccion: string | null,
): DetalleCampana {
  return {
    ...vistaDeCampana(campana, veterinarias, ahora, origen),
    descripcion: campana.descripcion,
    direccion,
    organizadorDetalle: {
      id: campana.organizadorId,
      nombre: campana.organizador,
      promedio: campana.promedio,
      totalResenas: campana.totalResenas,
    },
  };
}

export async function detalleDeCampana(id: number, origen: Punto | null, ahora: Ahora = ahoraEnLima()): Promise<DetalleCampana> {
  const [veterinarias, campanas] = await Promise.all([
    repositorio.cargarVeterinarias(),
    repositorio.cargarCampanasVigentes(ahora.fecha),
  ]);
  const campana = campanas.find((c) => c.id === id);
  if (!campana) throw new HttpError(404, MENSAJE_CAMPANA_NO_DISPONIBLE);

  const [direcciones] = await pool.query<RowDataPacket[]>('SELECT direccion FROM campana WHERE id = ?', [id]);
  return armarDetalleDeCampana(
    campana,
    veterinarias,
    ahora,
    origen,
    (direcciones[0]?.direccion as string | null | undefined) ?? null,
  );
}
