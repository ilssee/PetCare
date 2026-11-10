import { RowDataPacket } from 'mysql2';
import { pool } from '../../config/database';
import { CampanaRegistro, Dia, Horario, Servicio, VeterinariaRegistro } from './mapa.types';

/**
 * Capa de acceso a datos del módulo mapa. Carga veterinarias y campañas con sus especies, servicios
 * y horarios. Son pocas filas, así que el filtrado y el orden se hacen en mapa.logica.ts.
 */

function agrupar<T>(filas: RowDataPacket[], clave: string, valor: (fila: RowDataPacket) => T): Map<number, T[]> {
  const grupos = new Map<number, T[]>();
  for (const fila of filas) {
    const id = fila[clave] as number;
    if (!grupos.has(id)) grupos.set(id, []);
    grupos.get(id)!.push(valor(fila));
  }
  return grupos;
}

const aNumeroONulo = (valor: unknown): number | null => (valor === null || valor === undefined ? null : Number(valor));

export async function cargarVeterinarias(): Promise<VeterinariaRegistro[]> {
  const [veterinarias] = await pool.query<RowDataPacket[]>(
    `SELECT o.id, o.nombre, v.ubicacion_lat AS lat, v.ubicacion_long AS lng, v.tipo,
            (SELECT AVG(r.calificacion) FROM resena r WHERE r.organizador_id = o.id) AS promedio,
            (SELECT COUNT(*) FROM resena r WHERE r.organizador_id = o.id) AS totalResenas
     FROM veterinaria v
     JOIN organizador o ON o.id = v.organizador_id`,
  );
  const [especies] = await pool.query<RowDataPacket[]>(
    `SELECT ve.veterinaria_id, e.nombre
     FROM veterinaria_especie ve
     JOIN especie e ON e.id = ve.especie_id`,
  );
  const [servicios] = await pool.query<RowDataPacket[]>(
    `SELECT vs.veterinaria_id, s.id, s.nombre
     FROM veterinaria_servicio vs
     JOIN servicio s ON s.id = vs.servicio_id`,
  );
  const [horarios] = await pool.query<RowDataPacket[]>(
    `SELECT veterinaria_id, dia, hora_apertura, hora_cierre FROM horario_atencion`,
  );

  const especiesDe = agrupar(especies, 'veterinaria_id', (f) => f.nombre as string);
  const serviciosDe = agrupar<Servicio>(servicios, 'veterinaria_id', (f) => ({ id: f.id, nombre: f.nombre }));
  const horariosDe = agrupar<Horario>(horarios, 'veterinaria_id', (f) => ({
    dia: f.dia as Dia,
    apertura: f.hora_apertura as string,
    cierre: f.hora_cierre as string,
  }));

  return veterinarias.map((f) => ({
    id: f.id,
    nombre: f.nombre,
    lat: Number(f.lat),
    lng: Number(f.lng),
    tipo: f.tipo,
    promedio: aNumeroONulo(f.promedio),
    totalResenas: Number(f.totalResenas),
    especies: especiesDe.get(f.id) ?? [],
    servicios: serviciosDe.get(f.id) ?? [],
    horarios: horariosDe.get(f.id) ?? [],
  }));
}

/** Campañas que aún no terminaron (la fecha de hoy la decide quien llama, en hora de Lima). */
export async function cargarCampanasVigentes(hoy: string): Promise<CampanaRegistro[]> {
  const [campanas] = await pool.query<RowDataPacket[]>(
    `SELECT c.id, c.organizador_id, o.nombre AS organizador, c.nombre, c.descripcion,
            c.fecha_inicio, c.fecha_fin, c.hora_apertura, c.hora_cierre,
            c.ubicacion_lat AS lat, c.ubicacion_long AS lng, c.modalidad_costo AS costo,
            (SELECT AVG(r.calificacion) FROM resena r WHERE r.organizador_id = o.id) AS promedio,
            (SELECT COUNT(*) FROM resena r WHERE r.organizador_id = o.id) AS totalResenas,
            EXISTS (SELECT 1 FROM veterinaria v WHERE v.organizador_id = o.id) AS esDeVeterinaria
     FROM campana c
     JOIN organizador o ON o.id = c.organizador_id
     WHERE c.fecha_fin >= ?`,
    [hoy],
  );
  const [especies] = await pool.query<RowDataPacket[]>(
    `SELECT ce.campana_id, e.nombre
     FROM campana_especie ce
     JOIN especie e ON e.id = ce.especie_id`,
  );
  const [servicios] = await pool.query<RowDataPacket[]>(
    `SELECT cs.campana_id, s.id, s.nombre
     FROM campana_servicio cs
     JOIN servicio s ON s.id = cs.servicio_id`,
  );

  const especiesDe = agrupar(especies, 'campana_id', (f) => f.nombre as string);
  const serviciosDe = agrupar<Servicio>(servicios, 'campana_id', (f) => ({ id: f.id, nombre: f.nombre }));

  return campanas.map((f) => ({
    id: f.id,
    organizadorId: f.organizador_id,
    organizador: f.organizador,
    nombre: f.nombre,
    descripcion: f.descripcion,
    fechaInicio: f.fecha_inicio,
    fechaFin: f.fecha_fin,
    horaApertura: f.hora_apertura,
    horaCierre: f.hora_cierre,
    lat: Number(f.lat),
    lng: Number(f.lng),
    costo: f.costo,
    promedio: aNumeroONulo(f.promedio),
    totalResenas: Number(f.totalResenas),
    esDeVeterinaria: Boolean(f.esDeVeterinaria),
    especies: especiesDe.get(f.id) ?? [],
    servicios: serviciosDe.get(f.id) ?? [],
  }));
}

/** Catálogos para armar los filtros de la app. */
export async function cargarCatalogos(): Promise<{
  especies: string[];
  servicios: { id: number; nombre: string; categoria: string }[];
}> {
  const [especies] = await pool.query<RowDataPacket[]>('SELECT nombre FROM especie ORDER BY id');
  const [servicios] = await pool.query<RowDataPacket[]>(
    `SELECT s.id, s.nombre, c.nombre AS categoria
     FROM servicio s
     JOIN categoria_servicio c ON c.id = s.categoria_id
     ORDER BY c.id, s.id`,
  );
  return {
    especies: especies.map((f) => f.nombre as string),
    servicios: servicios.map((f) => ({ id: f.id, nombre: f.nombre, categoria: f.categoria })),
  };
}
