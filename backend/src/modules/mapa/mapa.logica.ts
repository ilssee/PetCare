import {
  Ahora,
  Area,
  CampanaMapa,
  CampanaRegistro,
  Dia,
  EstadoCampana,
  FiltrosCampanas,
  FiltrosVeterinarias,
  Horario,
  OrdenCampanas,
  OrdenVeterinarias,
  Punto,
  ResultadoBusqueda,
  VeterinariaMapa,
  VeterinariaRegistro,
} from './mapa.types';

/**
 * Reglas del mapa (HU08), sin acceso a la base de datos: estado de las campañas, disponibilidad,
 * distancias, filtros, orden y búsqueda por nombre. Así se pueden probar de forma aislada.
 */

const ZONA_HORARIA = 'America/Lima';

const DIA_POR_NOMBRE: Record<string, Dia> = {
  Monday: 'lunes',
  Tuesday: 'martes',
  Wednesday: 'miercoles',
  Thursday: 'jueves',
  Friday: 'viernes',
  Saturday: 'sabado',
  Sunday: 'domingo',
};

/** La fecha, el día de la semana y la hora actuales en Lima, sin depender de la zona del servidor. */
export function ahoraEnLima(instante: Date = new Date()): Ahora {
  const partes = new Intl.DateTimeFormat('en-US', {
    timeZone: ZONA_HORARIA,
    hourCycle: 'h23',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    weekday: 'long',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  }).formatToParts(instante);
  const valor = (tipo: string) => partes.find((p) => p.type === tipo)?.value ?? '';
  return {
    fecha: `${valor('year')}-${valor('month')}-${valor('day')}`,
    dia: DIA_POR_NOMBRE[valor('weekday')],
    hora: `${valor('hour')}:${valor('minute')}:${valor('second')}`,
  };
}

/** Próxima (aún no empieza), Activa (en curso) o Finalizada (ya terminó). */
export function estadoDeCampana(fechaInicio: string, fechaFin: string, hoy: string): EstadoCampana | 'finalizada' {
  if (hoy < fechaInicio) return 'proxima';
  if (hoy <= fechaFin) return 'activa';
  return 'finalizada';
}

/** Distancia en kilómetros entre dos puntos (fórmula de Haversine). */
export function distanciaKm(a: Punto, b: Punto): number {
  const RADIO_TIERRA_KM = 6371;
  const aRadianes = (grados: number) => (grados * Math.PI) / 180;
  const dLat = aRadianes(b.lat - a.lat);
  const dLng = aRadianes(b.lng - a.lng);
  const h =
    Math.sin(dLat / 2) ** 2 + Math.cos(aRadianes(a.lat)) * Math.cos(aRadianes(b.lat)) * Math.sin(dLng / 2) ** 2;
  return RADIO_TIERRA_KM * 2 * Math.asin(Math.sqrt(h));
}

export function dentroDelArea(area: Area, punto: Punto): boolean {
  return punto.lat <= area.norte && punto.lat >= area.sur && punto.lng <= area.este && punto.lng >= area.oeste;
}

/** Dos coordenadas se consideran la misma ubicación si coinciden hasta unos 10 cm. */
export function mismaUbicacion(a: Punto, b: Punto): boolean {
  return Math.abs(a.lat - b.lat) < 1e-6 && Math.abs(a.lng - b.lng) < 1e-6;
}

/** Quita tildes y mayúsculas para comparar textos ("Clínica" coincide con "clinica"). */
export function normalizar(texto: string): string {
  return texto
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .trim();
}

/** Una veterinaria está abierta si hoy tiene una franja que incluye la hora actual. */
export function estaAbierta(horarios: Horario[], ahora: Ahora): boolean {
  return horarios.some((h) => h.dia === ahora.dia && h.apertura <= ahora.hora && ahora.hora < h.cierre);
}

/** Una campaña está disponible ahora si está Activa y la hora actual cae dentro de su horario. */
export function campanaDisponibleAhora(c: CampanaRegistro, ahora: Ahora): boolean {
  return (
    estadoDeCampana(c.fechaInicio, c.fechaFin, ahora.fecha) === 'activa' &&
    c.horaApertura <= ahora.hora &&
    ahora.hora < c.horaCierre
  );
}

/** Solo las campañas Próximas o Activas se muestran; las finalizadas se descartan. */
export function campanasVigentes(campanas: CampanaRegistro[], ahora: Ahora): CampanaRegistro[] {
  return campanas.filter((c) => estadoDeCampana(c.fechaInicio, c.fechaFin, ahora.fecha) !== 'finalizada');
}

function estadoVigente(c: CampanaRegistro, ahora: Ahora): EstadoCampana {
  return estadoDeCampana(c.fechaInicio, c.fechaFin, ahora.fecha) as EstadoCampana;
}

function redondearKm(km: number): number {
  return Math.round(km * 10) / 10;
}

export function vistaDeVeterinaria(
  v: VeterinariaRegistro,
  campanas: CampanaRegistro[],
  ahora: Ahora,
  origen: Punto | null,
): VeterinariaMapa {
  return {
    id: v.id,
    nombre: v.nombre,
    tipo: v.tipo,
    lat: v.lat,
    lng: v.lng,
    promedio: v.promedio,
    totalResenas: v.totalResenas,
    distanciaKm: origen ? redondearKm(distanciaKm(origen, v)) : null,
    abiertaAhora: estaAbierta(v.horarios, ahora),
    horarioHoy: v.horarios
      .filter((h) => h.dia === ahora.dia)
      .sort((a, b) => a.apertura.localeCompare(b.apertura))
      .map((h) => ({ apertura: h.apertura, cierre: h.cierre })),
    especies: v.especies,
    servicios: v.servicios,
    campanasAqui: campanasVigentes(campanas, ahora)
      .filter((c) => c.organizadorId === v.id && mismaUbicacion(c, v))
      .map((c) => ({ id: c.id, estado: estadoVigente(c, ahora) })),
  };
}

export function vistaDeCampana(
  c: CampanaRegistro,
  veterinarias: VeterinariaRegistro[],
  ahora: Ahora,
  origen: Punto | null,
): CampanaMapa {
  const veterinaria = c.esDeVeterinaria ? veterinarias.find((v) => v.id === c.organizadorId) : undefined;
  return {
    id: c.id,
    nombre: c.nombre,
    organizador: c.organizador,
    estado: estadoVigente(c, ahora),
    lat: c.lat,
    lng: c.lng,
    fechaInicio: c.fechaInicio,
    fechaFin: c.fechaFin,
    horaApertura: c.horaApertura,
    horaCierre: c.horaCierre,
    costo: c.costo,
    promedio: c.promedio,
    distanciaKm: origen ? redondearKm(distanciaKm(origen, c)) : null,
    disponibleAhora: campanaDisponibleAhora(c, ahora),
    especies: c.especies,
    servicios: c.servicios,
    veterinariaId: veterinaria ? veterinaria.id : null,
    enUbicacionDeVeterinaria: veterinaria ? mismaUbicacion(c, veterinaria) : false,
  };
}

// ---------------------------------------------------------------------
// Orden
// ---------------------------------------------------------------------

const porNombre = (a: { nombre: string }, b: { nombre: string }) => a.nombre.localeCompare(b.nombre, 'es');

/** Mayor calificación primero; las que no tienen reseñas quedan al final. */
function porCalificacion(a: { promedio: number | null; nombre: string }, b: { promedio: number | null; nombre: string }) {
  if (a.promedio === null && b.promedio === null) return porNombre(a, b);
  if (a.promedio === null) return 1;
  if (b.promedio === null) return -1;
  return b.promedio - a.promedio || porNombre(a, b);
}

function porDistancia(a: { distanciaKm: number | null; nombre: string }, b: { distanciaKm: number | null; nombre: string }) {
  return (a.distanciaKm ?? Infinity) - (b.distanciaKm ?? Infinity) || porNombre(a, b);
}

// ---------------------------------------------------------------------
// Consultas
// ---------------------------------------------------------------------

function intersecta(elegidos: string[], disponibles: string[]): boolean {
  return elegidos.length === 0 || elegidos.some((e) => disponibles.includes(e));
}

function ofreceServicio(elegidos: number[], servicios: { id: number }[]): boolean {
  return elegidos.length === 0 || servicios.some((s) => elegidos.includes(s.id));
}

export interface OpcionesVeterinarias {
  filtros: FiltrosVeterinarias;
  orden: OrdenVeterinarias;
  origen: Punto | null;
  area: Area | null;
  ahora: Ahora;
}

/** RF27, RF30, RF32, RF33 y RF36 para veterinarias. */
export function listarVeterinarias(
  veterinarias: VeterinariaRegistro[],
  campanas: CampanaRegistro[],
  o: OpcionesVeterinarias,
): VeterinariaMapa[] {
  const { filtros } = o;
  const vistas = veterinarias
    .filter((v) => !o.area || dentroDelArea(o.area, v))
    .filter((v) => filtros.tipos.length === 0 || filtros.tipos.includes(v.tipo))
    .filter((v) => intersecta(filtros.especies, v.especies))
    .filter((v) => ofreceServicio(filtros.servicios, v.servicios))
    .filter((v) => !filtros.disponibleAhora || estaAbierta(v.horarios, o.ahora))
    .map((v) => vistaDeVeterinaria(v, campanas, o.ahora, o.origen));
  const criterio = o.orden === 'distancia' && o.origen ? porDistancia : o.orden === 'calificacion' ? porCalificacion : porNombre;
  return vistas.sort(criterio);
}

export interface OpcionesCampanas {
  filtros: FiltrosCampanas;
  orden: OrdenCampanas;
  origen: Punto | null;
  area: Area | null;
  ahora: Ahora;
}

/** RF27, RF30, RF34, RF35 y RF36 para campañas. Las finalizadas nunca se muestran. */
export function listarCampanas(
  campanas: CampanaRegistro[],
  veterinarias: VeterinariaRegistro[],
  o: OpcionesCampanas,
): CampanaMapa[] {
  const { filtros } = o;
  const vistas = campanasVigentes(campanas, o.ahora)
    .filter((c) => !o.area || dentroDelArea(o.area, c))
    .filter((c) => filtros.estados.length === 0 || filtros.estados.includes(estadoVigente(c, o.ahora)))
    .filter((c) => intersecta(filtros.especies, c.especies))
    .filter((c) => ofreceServicio(filtros.servicios, c.servicios))
    .filter((c) => filtros.costos.length === 0 || filtros.costos.includes(c.costo))
    .filter((c) => !filtros.disponibleAhora || campanaDisponibleAhora(c, o.ahora))
    .map((c) => vistaDeCampana(c, veterinarias, o.ahora, o.origen));

  const criterios: Record<OrdenCampanas, (a: CampanaMapa, b: CampanaMapa) => number> = {
    inicio: (a, b) => a.fechaInicio.localeCompare(b.fechaInicio) || porNombre(a, b),
    fin: (a, b) => a.fechaFin.localeCompare(b.fechaFin) || porNombre(a, b),
    distancia: porDistancia,
    calificacion: porCalificacion,
  };
  const orden = o.orden === 'distancia' && !o.origen ? 'inicio' : o.orden;
  return vistas.sort(criterios[orden]);
}

export interface OpcionesBusqueda {
  origen: Punto | null;
  area: Area | null;
  ahora: Ahora;
}

/**
 * RF29: busca por nombre entre veterinarias, entidades organizadoras y campañas.
 *  - Coincide una veterinaria: aparece esa veterinaria.
 *  - Coincide una campaña (o su organizador) de una veterinaria: aparece la veterinaria, sin repetirla.
 *  - Coincide una campaña (o su organizador) sin veterinaria: aparece la campaña.
 */
export function buscarPorNombre(
  texto: string,
  veterinarias: VeterinariaRegistro[],
  campanas: CampanaRegistro[],
  o: OpcionesBusqueda,
): ResultadoBusqueda[] {
  const buscado = normalizar(texto);
  const vigentes = campanasVigentes(campanas, o.ahora);
  const veterinariasHalladas = new Map<number, VeterinariaRegistro>();
  const campanasHalladas: CampanaRegistro[] = [];

  for (const v of veterinarias) {
    if (normalizar(v.nombre).includes(buscado)) veterinariasHalladas.set(v.id, v);
  }
  for (const c of vigentes) {
    if (!normalizar(c.nombre).includes(buscado) && !normalizar(c.organizador).includes(buscado)) continue;
    const veterinaria = c.esDeVeterinaria ? veterinarias.find((v) => v.id === c.organizadorId) : undefined;
    if (veterinaria) veterinariasHalladas.set(veterinaria.id, veterinaria);
    else campanasHalladas.push(c);
  }

  const resultados: ResultadoBusqueda[] = [
    ...[...veterinariasHalladas.values()].map((v) => ({
      clase: 'veterinaria' as const,
      ...vistaDeVeterinaria(v, vigentes, o.ahora, o.origen),
    })),
    ...campanasHalladas.map((c) => ({
      clase: 'campana' as const,
      ...vistaDeCampana(c, veterinarias, o.ahora, o.origen),
    })),
  ].filter((r) => !o.area || dentroDelArea(o.area, r));

  return resultados.sort(o.origen ? porDistancia : porNombre);
}
