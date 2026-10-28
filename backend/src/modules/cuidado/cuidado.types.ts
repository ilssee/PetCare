export type ModoTermino = 'una_vez' | 'varias_veces' | 'hasta_fecha' | 'indefinido';
export type UnidadFrecuencia = 'dias' | 'semanas' | 'meses' | 'anios';
export type EstadoCuidado = 'pendiente' | 'finalizado' | 'eliminado';

/** Cómo y cuándo se repite un cuidado. Lo define el usuario (con su veterinario), no el sistema. */
export interface ConfiguracionCuidado {
  modoTermino: ModoTermino;
  frecuenciaValor: number | null; // null si el modo es "una vez"
  frecuenciaUnidad: UnidadFrecuencia | null;
  totalRepeticiones: number | null; // solo en "varias veces"
  fechaLimite: string | null; // AAAA-MM-DD, solo en "hasta una fecha"
  fechaInicio: string; // AAAA-MM-DD: pasa a ser la primera fecha recurrente
  avisoAnticipado: boolean;
  nota: string | null;
}

/** Cuidado listo para guardarse. reglaId es null si lo creó el usuario (cuidado personalizado). */
export interface NuevoCuidado extends ConfiguracionCuidado {
  reglaId: number | null;
  nombre: string;
  descripcion: string;
}

/** Cuidado tal como se devuelve a la app. */
export interface CuidadoRespuesta {
  id: number;
  mascotaId: number;
  reglaId: number | null;
  nombre: string;
  descripcion: string;
  nota: string | null;
  modoTermino: ModoTermino;
  frecuenciaValor: number | null;
  frecuenciaUnidad: UnidadFrecuencia | null;
  totalRepeticiones: number | null;
  fechaLimite: string | null;
  fechaRecurrente: string; // próxima fecha del cuidado
  avisoAnticipado: boolean;
  estado: EstadoCuidado;
}
