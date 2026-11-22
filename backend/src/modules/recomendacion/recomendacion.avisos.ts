import { Notificacion } from '../recordatorio/recordatorio.avisos';

/** Una recomendación nueva que le corresponde a una mascota activa y de la que aún no se le avisó. */
export interface Coincidencia {
  usuarioId: number;
  mascotaId: number;
  mascota: string;
  reglaId: number;
  especie?: string; // la app la usa para elegir el ícono del aviso
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
    const { usuarioId, mascotaId, mascota, especie } = grupo[0];
    const cantidad = grupo.length;
    const reglasIds = grupo.map((c) => c.reglaId);
    return {
      mascotaId,
      reglasIds,
      aviso: {
        usuarioId,
        cuidadoId: 0,
        tipo: 'recomendacion',
        titulo: `Nuevas recomendaciones para ${mascota}`,
        cuerpo:
          cantidad === 1
            ? 'Tenemos 1 nuevo cuidado preventivo para recomendarte.'
            : `Tenemos ${cantidad} nuevos cuidados preventivos para recomendarte.`,
        ...(especie ? { especie } : {}),
        // Al tocarlo se abre la información preventiva de la mascota con estas recomendaciones desplegadas.
        mascotaId,
        reglasIds,
      },
    };
  });
}
