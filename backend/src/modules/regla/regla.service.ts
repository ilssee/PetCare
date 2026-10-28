import * as mascotas from '../mascota/mascota.service';
import * as repositorio from './regla.repository';
import { ReglaConEstado, ReglaDisponible } from './regla.types';

/**
 * Cuidados recomendados para una mascota (RF09 propuesta de cronograma y RF24 información preventiva).
 * Si la mascota no existe o es de otro usuario, responde 404 igual que el módulo mascota.
 */
export async function listarDisponibles(mascotaId: number, usuarioId: number): Promise<ReglaDisponible[]> {
  const mascota = await mascotas.obtener(mascotaId, usuarioId);
  return repositorio.listarDisponibles(mascota.id);
}

/** RF24: todas las recomendaciones de la mascota, marcando las que ya están en su cronograma. */
export async function listarConEstado(mascotaId: number, usuarioId: number): Promise<ReglaConEstado[]> {
  const mascota = await mascotas.obtener(mascotaId, usuarioId);
  return repositorio.listarConEstado(mascota.id);
}
