/** Tipos del módulo mapa (HU08): veterinarias y campañas que se muestran en el mapa. */

export type TipoVeterinaria = 'municipal' | 'privada';
export type EstadoCampana = 'proxima' | 'activa';
export type CostoCampana = 'gratuita' | 'de_pago';
export type Dia = 'lunes' | 'martes' | 'miercoles' | 'jueves' | 'viernes' | 'sabado' | 'domingo';

export interface Servicio {
  id: number;
  nombre: string;
}

export interface Horario {
  dia: Dia;
  apertura: string; // HH:MM:SS
  cierre: string; // HH:MM:SS
}

/** Una veterinaria tal como está guardada, con todo lo que se necesita para filtrarla. */
export interface VeterinariaRegistro {
  id: number;
  nombre: string;
  lat: number;
  lng: number;
  tipo: TipoVeterinaria;
  promedio: number | null; // promedio de calificación; null si no tiene reseñas
  totalResenas: number;
  especies: string[];
  servicios: Servicio[];
  horarios: Horario[];
}

/** Una campaña vigente (Próxima o Activa) tal como está guardada. */
export interface CampanaRegistro {
  id: number;
  organizadorId: number;
  organizador: string;
  nombre: string;
  descripcion: string;
  fechaInicio: string; // AAAA-MM-DD
  fechaFin: string; // AAAA-MM-DD
  horaApertura: string; // HH:MM:SS
  horaCierre: string; // HH:MM:SS
  lat: number;
  lng: number;
  costo: CostoCampana;
  promedio: number | null; // promedio de calificación del organizador
  esDeVeterinaria: boolean; // el organizador es una veterinaria
  especies: string[];
  servicios: Servicio[];
}

/** Momento actual en Lima, ya separado en fecha, día de la semana y hora. */
export interface Ahora {
  fecha: string; // AAAA-MM-DD
  dia: Dia;
  hora: string; // HH:MM:SS
}

export interface Punto {
  lat: number;
  lng: number;
}

/** Área visible del mapa. */
export interface Area {
  norte: number;
  sur: number;
  este: number;
  oeste: number;
}

export interface FiltrosVeterinarias {
  tipos: TipoVeterinaria[];
  especies: string[];
  servicios: number[];
  disponibleAhora: boolean;
}

export interface FiltrosCampanas {
  estados: EstadoCampana[];
  especies: string[];
  servicios: number[];
  costos: CostoCampana[];
  disponibleAhora: boolean;
}

export type OrdenVeterinarias = 'distancia' | 'calificacion' | 'nombre';
export type OrdenCampanas = 'inicio' | 'fin' | 'distancia' | 'calificacion';

/** Lo que recibe la app por cada veterinaria. */
export interface VeterinariaMapa {
  id: number;
  nombre: string;
  tipo: TipoVeterinaria;
  lat: number;
  lng: number;
  promedio: number | null;
  totalResenas: number;
  distanciaKm: number | null;
  abiertaAhora: boolean;
  /** Franjas de atención de hoy (vacía si hoy no atiende). */
  horarioHoy: { apertura: string; cierre: string }[];
  especies: string[];
  servicios: Servicio[];
  /** Campañas vigentes de esta veterinaria que están en su misma ubicación (RF28). */
  campanasAqui: { id: number; estado: EstadoCampana }[];
}

/** Lo que recibe la app por cada campaña. */
export interface CampanaMapa {
  id: number;
  nombre: string;
  organizador: string;
  estado: EstadoCampana;
  lat: number;
  lng: number;
  fechaInicio: string;
  fechaFin: string;
  horaApertura: string;
  horaCierre: string;
  costo: CostoCampana;
  promedio: number | null;
  distanciaKm: number | null;
  disponibleAhora: boolean;
  especies: string[];
  servicios: Servicio[];
  /** Id de la veterinaria que organiza la campaña, o null si la organiza una entidad externa. */
  veterinariaId: number | null;
  /** La campaña está en la misma ubicación de su veterinaria: se muestran juntas en un solo marcador (RF28). */
  enUbicacionDeVeterinaria: boolean;
}

/**
 * Resultado de la búsqueda por nombre (RF29). "clase" es la etiqueta Veterinaria o Campaña;
 * no se llama "tipo" porque la veterinaria ya tiene un tipo (municipal o privada).
 */
export type ResultadoBusqueda =
  | ({ clase: 'veterinaria' } & VeterinariaMapa)
  | ({ clase: 'campana' } & CampanaMapa);
