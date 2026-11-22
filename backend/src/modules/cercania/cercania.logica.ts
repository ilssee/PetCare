import { Notificacion } from '../recordatorio/recordatorio.avisos';
import { distanciaKm } from '../mapa/mapa.logica';
import { LugarNuevo, UsuarioCercano } from './cercania.types';

/**
 * Reglas de los avisos de cercanía (RF39 y RF40), sin acceso a la base de datos ni a Firebase:
 * deciden a quién avisar de un lugar nuevo y con qué mensaje.
 */

function formatearKm(km: number): string {
  return (Math.round(km * 10) / 10).toFixed(1);
}

/**
 * Un usuario recibe el aviso si activó ese tipo de aviso y el lugar queda dentro de su radio
 * (la distancia igual al radio también cuenta).
 */
export function avisosDeLugar(lugar: LugarNuevo, usuarios: UsuarioCercano[]): Notificacion[] {
  const avisos: Notificacion[] = [];
  for (const usuario of usuarios) {
    const quiere = lugar.tipo === 'campana' ? usuario.notificarCampanas : usuario.notificarVeterinarias;
    if (!quiere) continue;
    const km = distanciaKm({ lat: usuario.lat, lng: usuario.lng }, lugar);
    if (km > usuario.radioKm) continue;

    const distancia = `a ${formatearKm(km)} km de tu ubicación`;
    avisos.push(
      lugar.tipo === 'campana'
        ? {
            usuarioId: usuario.usuarioId,
            cuidadoId: 0,
            tipo: 'campana_cercana',
            titulo: 'Nueva campaña cerca de ti',
            cuerpo: `${lugar.nombre} (${lugar.organizador}), ${distancia}.`,
            ...(lugar.variante ? { variante: lugar.variante } : {}),
            lugarId: lugar.id,
            ...(lugar.veterinariaId ? { veterinariaId: lugar.veterinariaId } : {}),
          }
        : {
            usuarioId: usuario.usuarioId,
            cuidadoId: 0,
            tipo: 'veterinaria_cercana',
            titulo: 'Nueva veterinaria cerca de ti',
            cuerpo: `${lugar.nombre}, ${distancia}.`,
            ...(lugar.variante ? { variante: lugar.variante } : {}),
            lugarId: lugar.id,
            veterinariaId: lugar.id,
          },
    );
  }
  return avisos;
}
