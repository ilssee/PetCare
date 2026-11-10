/** Tipos del módulo cercanía (HU09): ubicación de referencia, radio de interés y avisos de lugares nuevos. */

/** Lo que el usuario configuró (RF37 y RF38). Ubicación y radio son opcionales. */
export interface ConfiguracionCercania {
  lat: number | null;
  lng: number | null;
  radioKm: number | null;
  notificarCampanas: boolean;
  notificarVeterinarias: boolean;
}

/** Un usuario con su configuración, para decidir a quién avisar. */
export interface UsuarioCercano {
  usuarioId: number;
  lat: number;
  lng: number;
  radioKm: number;
  notificarCampanas: boolean;
  notificarVeterinarias: boolean;
}

/** Una campaña o veterinaria que todavía no se avisó (su fecha de notificación está vacía). */
export interface LugarNuevo {
  tipo: 'campana' | 'veterinaria';
  id: number;
  nombre: string;
  organizador: string; // para una veterinaria es su propio nombre
  lat: number;
  lng: number;
}
