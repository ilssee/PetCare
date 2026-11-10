import { HttpError } from '../../common/errors/http-error';
import { ahoraEnLima, buscarPorNombre, listarCampanas, listarVeterinarias } from './mapa.logica';
import * as repositorio from './mapa.repository';
import {
  Area,
  CampanaMapa,
  CostoCampana,
  EstadoCampana,
  OrdenCampanas,
  OrdenVeterinarias,
  Punto,
  ResultadoBusqueda,
  TipoVeterinaria,
  VeterinariaMapa,
} from './mapa.types';

/**
 * Reglas del módulo mapa (HU08). Aquí se validan los datos que llegan en la URL; el cálculo de filtros,
 * orden y búsqueda está en mapa.logica.ts. Los parámetros están descritos en mapa.routes.ts.
 */

type Consulta = Record<string, unknown>;

const TIPOS: TipoVeterinaria[] = ['municipal', 'privada'];
const ESTADOS: EstadoCampana[] = ['proxima', 'activa'];
const COSTOS: CostoCampana[] = ['gratuita', 'de_pago'];
const ORDEN_VETERINARIAS: OrdenVeterinarias[] = ['distancia', 'calificacion', 'nombre'];
const ORDEN_CAMPANAS: OrdenCampanas[] = ['inicio', 'fin', 'distancia', 'calificacion'];

const MENSAJE_UBICACION = 'Activa tu ubicación para ordenar por distancia';

/** "a,b,c" -> ['a','b','c']; si no viene, lista vacía. */
function lista(valor: unknown): string[] {
  if (valor === undefined || valor === null) return [];
  return String(valor)
    .split(',')
    .map((v) => v.trim())
    .filter((v) => v !== '');
}

function listaDeOpciones<T extends string>(valor: unknown, permitidos: T[], nombre: string): T[] {
  return lista(valor).map((v) => {
    if (!permitidos.includes(v as T)) throw new HttpError(400, `El valor "${v}" no es válido para ${nombre}`);
    return v as T;
  });
}

function listaDeIds(valor: unknown): number[] {
  return lista(valor).map((v) => {
    const id = Number(v);
    if (!Number.isInteger(id) || id <= 0) throw new HttpError(400, `El servicio "${v}" no es válido`);
    return id;
  });
}

function opcion<T extends string>(valor: unknown, permitidos: T[], porDefecto: T, nombre: string): T {
  if (valor === undefined || valor === '') return porDefecto;
  if (!permitidos.includes(valor as T)) throw new HttpError(400, `El orden "${valor}" no es válido para ${nombre}`);
  return valor as T;
}

function esVerdadero(valor: unknown): boolean {
  return valor === 'true' || valor === '1';
}

function numero(valor: unknown, nombre: string, minimo: number, maximo: number): number {
  const n = Number(valor);
  if (valor === '' || !Number.isFinite(n) || n < minimo || n > maximo) {
    throw new HttpError(400, `${nombre} no es válida`);
  }
  return n;
}

/** Ubicación del usuario (lat y lng juntas). Sirve para calcular y ordenar por distancia. */
function leerOrigen(c: Consulta): Punto | null {
  if (c.lat === undefined && c.lng === undefined) return null;
  if (c.lat === undefined || c.lng === undefined) throw new HttpError(400, 'Envía la latitud y la longitud juntas');
  return { lat: numero(c.lat, 'La latitud', -90, 90), lng: numero(c.lng, 'La longitud', -180, 180) };
}

/** Área visible del mapa (norte, sur, este y oeste juntos). */
function leerArea(c: Consulta): Area | null {
  const claves = ['norte', 'sur', 'este', 'oeste'];
  const enviadas = claves.filter((k) => c[k] !== undefined);
  if (enviadas.length === 0) return null;
  if (enviadas.length < claves.length) throw new HttpError(400, 'Envía norte, sur, este y oeste juntos');
  const area = {
    norte: numero(c.norte, 'El límite norte', -90, 90),
    sur: numero(c.sur, 'El límite sur', -90, 90),
    este: numero(c.este, 'El límite este', -180, 180),
    oeste: numero(c.oeste, 'El límite oeste', -180, 180),
  };
  if (area.norte < area.sur) throw new HttpError(400, 'El límite norte debe estar sobre el límite sur');
  return area;
}

export async function veterinarias(c: Consulta): Promise<VeterinariaMapa[]> {
  const origen = leerOrigen(c);
  const area = leerArea(c);
  const filtros = {
    tipos: listaDeOpciones(c.tipo, TIPOS, 'el tipo'),
    especies: lista(c.especie),
    servicios: listaDeIds(c.servicio),
    disponibleAhora: esVerdadero(c.disponibleAhora),
  };
  const orden = opcion(c.ordenar, ORDEN_VETERINARIAS, origen ? 'distancia' : 'nombre', 'las veterinarias');
  if (orden === 'distancia' && !origen) throw new HttpError(400, MENSAJE_UBICACION);

  const ahora = ahoraEnLima();
  const [vets, campanas] = await Promise.all([
    repositorio.cargarVeterinarias(),
    repositorio.cargarCampanasVigentes(ahora.fecha),
  ]);
  return listarVeterinarias(vets, campanas, { filtros, orden, origen, area, ahora });
}

export async function campanas(c: Consulta): Promise<CampanaMapa[]> {
  const origen = leerOrigen(c);
  const area = leerArea(c);
  const filtros = {
    estados: listaDeOpciones(c.estado, ESTADOS, 'el estado'),
    especies: lista(c.especie),
    servicios: listaDeIds(c.servicio),
    costos: listaDeOpciones(c.costo, COSTOS, 'el costo'),
    disponibleAhora: esVerdadero(c.disponibleAhora),
  };
  const orden = opcion(c.ordenar, ORDEN_CAMPANAS, 'inicio', 'las campañas');
  if (orden === 'distancia' && !origen) throw new HttpError(400, MENSAJE_UBICACION);

  const ahora = ahoraEnLima();
  const [vets, camps] = await Promise.all([
    repositorio.cargarVeterinarias(),
    repositorio.cargarCampanasVigentes(ahora.fecha),
  ]);
  return listarCampanas(camps, vets, { filtros, orden, origen, area, ahora });
}

export async function buscar(c: Consulta): Promise<ResultadoBusqueda[]> {
  const texto = String(c.texto ?? '').trim();
  if (texto === '') throw new HttpError(400, 'Escribe un nombre para buscar');
  if (texto.length > 100) throw new HttpError(400, 'El texto de búsqueda no puede pasar de 100 caracteres');

  const origen = leerOrigen(c);
  const area = leerArea(c);
  const ahora = ahoraEnLima();
  const [vets, camps] = await Promise.all([
    repositorio.cargarVeterinarias(),
    repositorio.cargarCampanasVigentes(ahora.fecha),
  ]);
  return buscarPorNombre(texto, vets, camps, { origen, area, ahora });
}

/** Opciones para los filtros de la app: especies y servicios agrupados por categoría. */
export async function filtros() {
  return repositorio.cargarCatalogos();
}
