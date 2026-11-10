import { Notificacion } from '../recordatorio/recordatorio.avisos';

/** Una recomendación nueva que le corresponde a una mascota activa y de la que aún no se le avisó. */
export interface Coincidencia {
  usuarioId: number;
  mascotaId: number;
  mascota: string;
  reglaId: number;
}

/** Recomendaciones nuevas de una mascota, juntas para avisar una sola vez. */
export interface AvisoDeMascota {
  mascotaId: number;
  reglasIds: number[];
  aviso: Notificacion;
}

/** RF26: agrupa las coincidencias por mascota y arma una notificación por cada una. */
export function agruparAvisos(coincidencias: Coincidencia[]): AvisoDeMascota[] {
  const porMascota = new Map<number, Coincidencia[]>();
  for (const c of coincidencias) {
    porMascota.set(c.mascotaId, [...(porMascota.get(c.mascotaId) ?? []), c]);
  }

  return [...porMascota.values()].map((grupo) => {
    const { usuarioId, mascotaId, mascota } = grupo[0];
    const cantidad = grupo.length;
    return {
      mascotaId,
      reglasIds: grupo.map((c) => c.reglaId),
      aviso: {
        usuarioId,
        cuidadoId: 0,
        tipo: 'recomendacion',
        titulo: `Nuevas recomendaciones para ${mascota}`,
        cuerpo:
          cantidad === 1
            ? `Hay 1 nueva recomendación preventiva disponible para ${mascota}.`
            : `Hay ${cantidad} nuevas recomendaciones preventivas disponibles para ${mascota}.`,
      },
    };
  });
}
