import * as mascotas from '../mascota/mascota.service';
import * as repositorio from './regla.repository';
import { ReglaDisponible } from './regla.types';

/**
 * Cuidados recomendados para una mascota (RF09 propuesta de cronograma y RF24 recomendaciones).
 * Si la mascota no existe o es de otro usuario, responde 404 igual que el módulo mascota.
 */
export async function listarDisponibles(mascotaId: number, usuarioId: number): Promise<ReglaDisponible[]> {
  const mascota = await mascotas.obtener(mascotaId, usuarioId);
  return repositorio.listarDisponibles(mascota.id);
}
