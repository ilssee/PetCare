import { RowDataPacket } from 'mysql2';
import { HttpError } from '../../common/errors/http-error';
import { pool } from '../../config/database';
import * as resenas from '../resena/resena.repository';
import { ResenaVista } from '../resena/resena.types';
import { ahoraEnLima, vistaDeCampana, vistaDeVeterinaria } from './mapa.logica';
import * as repositorio from './mapa.repository';
import { Ahora, CampanaMapa, Dia, Horario, Punto, VeterinariaMapa } from './mapa.types';

/**
 * Perfil completo de una veterinaria (RF41): todo lo que muestra su pantalla en una sola consulta.
 * Las campañas de la veterinaria se entregan con su estado, costo y fechas; su detalle es de otra pantalla.
 */

const DIAS: Dia[] = ['lunes', 'martes', 'miercoles', 'jueves', 'viernes', 'sabado', 'domingo'];

/** Cuántas reseñas recientes se adelantan en la pestaña Reseñas del perfil. */
const RESENAS_RECIENTES = 2;

export interface Franja {
  apertura: string;
  cierre: string;
}

/** Días seguidos con el mismo horario ("lunes a viernes, 9:00 a 19:00"). Sin franjas significa que no atiende. */
export interface GrupoHorario {
  desde: Dia;
  hasta: Dia;
  franjas: Franja[];
  incluyeHoy: boolean;
}

export interface ServiciosDeCategoria {
  categoria: string;
  servicios: string[];
}

export interface PerfilVeterinaria extends VeterinariaMapa {
  direccion: string | null;
  horarioSemana: GrupoHorario[];
  serviciosPorCategoria: ServiciosDeCategoria[];
  telefonos: string[];
  redes: string[];
  fotos: string[]; // rutas de las imágenes guardadas en el servidor
  /** Campañas de la veterinaria; con la descripción y la dirección para desplegarlas en el perfil (RF43). */
  campanas: (CampanaMapa & { descripcion: string; direccion: string | null })[];
  resenas: { promedio: number | null; total: number; recientes: ResenaVista[] };
}

/**
 * Junta los días consecutivos que tienen exactamente el mismo horario, de lunes a domingo.
 * Un día sin atención también se agrupa (por ejemplo, "domingo: cerrado").
 */
export function agruparHorarioSemanal(horarios: Horario[], hoy: Dia): GrupoHorario[] {
  const porDia = DIAS.map((dia) => ({
    dia,
    franjas: horarios
      .filter((h) => h.dia === dia)
      .sort((a, b) => a.apertura.localeCompare(b.apertura))
      .map((h) => ({ apertura: h.apertura, cierre: h.cierre })),
  }));

  const grupos: GrupoHorario[] = [];
  for (const { dia, franjas } of porDia) {
    const ultimo = grupos[grupos.length - 1];
    if (ultimo && JSON.stringify(ultimo.franjas) === JSON.stringify(franjas)) {
      ultimo.hasta = dia;
      ultimo.incluyeHoy = ultimo.incluyeHoy || dia === hoy;
    } else {
      grupos.push({ desde: dia, hasta: dia, franjas, incluyeHoy: dia === hoy });
    }
  }
  return grupos;
}

async function extras(id: number) {
  const [telefonos] = await pool.query<RowDataPacket[]>(
    'SELECT telefono FROM veterinaria_telefono WHERE veterinaria_id = ? ORDER BY id',
    [id],
  );
  const [redes] = await pool.query<RowDataPacket[]>(
    'SELECT enlace FROM veterinaria_red_social WHERE veterinaria_id = ? ORDER BY id',
    [id],
  );
  const [fotos] = await pool.query<RowDataPacket[]>(
    'SELECT ruta FROM veterinaria_foto WHERE veterinaria_id = ? ORDER BY id',
    [id],
  );
  const [servicios] = await pool.query<RowDataPacket[]>(
    `SELECT s.nombre, c.nombre AS categoria
     FROM veterinaria_servicio vs
     JOIN servicio s ON s.id = vs.servicio_id
     JOIN categoria_servicio c ON c.id = s.categoria_id
     WHERE vs.veterinaria_id = ?
     ORDER BY c.id, s.id`,
    [id],
  );
  const [direccion] = await pool.query<RowDataPacket[]>(
    'SELECT direccion FROM veterinaria WHERE organizador_id = ?',
    [id],
  );
  const [direccionesCampanas] = await pool.query<RowDataPacket[]>(
    'SELECT id, direccion FROM campana WHERE organizador_id = ?',
    [id],
  );
  return {
    direccion: (direccion[0]?.direccion as string | null | undefined) ?? null,
    direccionesCampanas: new Map(direccionesCampanas.map((f) => [f.id as number, (f.direccion as string | null) ?? null])),
    telefonos: telefonos.map((f) => f.telefono as string),
    redes: redes.map((f) => f.enlace as string),
    fotos: fotos.map((f) => f.ruta as string),
    servicios: servicios.map((f) => ({ nombre: f.nombre as string, categoria: f.categoria as string })),
  };
}

/** Agrupa los servicios por categoría, respetando el orden del catálogo. */
export function agruparServicios(servicios: { nombre: string; categoria: string }[]): ServiciosDeCategoria[] {
  const grupos: ServiciosDeCategoria[] = [];
  for (const s of servicios) {
    const grupo = grupos.find((g) => g.categoria === s.categoria);
    if (grupo) grupo.servicios.push(s.nombre);
    else grupos.push({ categoria: s.categoria, servicios: [s.nombre] });
  }
  return grupos;
}

export async function perfilDeVeterinaria(
  id: number,
  usuarioId: number,
  origen: Punto | null,
  ahora: Ahora = ahoraEnLima(),
): Promise<PerfilVeterinaria> {
  const [veterinarias, campanas, detalle, reseñas] = await Promise.all([
    repositorio.cargarVeterinarias(),
    repositorio.cargarCampanasVigentes(ahora.fecha),
    extras(id),
    resenas.listar(id, usuarioId, 'recientes', RESENAS_RECIENTES),
  ]);
  const veterinaria = veterinarias.find((v) => v.id === id);
  if (!veterinaria) throw new HttpError(404, 'No se encontró la veterinaria');

  const propias = campanas
    .filter((c) => c.organizadorId === id)
    .sort((a, b) => a.fechaInicio.localeCompare(b.fechaInicio) || a.nombre.localeCompare(b.nombre, 'es'));

  return {
    ...vistaDeVeterinaria(veterinaria, campanas, ahora, origen),
    direccion: detalle.direccion,
    horarioSemana: agruparHorarioSemanal(veterinaria.horarios, ahora.dia),
    serviciosPorCategoria: agruparServicios(detalle.servicios),
    telefonos: detalle.telefonos,
    redes: detalle.redes,
    fotos: detalle.fotos,
    campanas: propias.map((c) => ({
      ...vistaDeCampana(c, veterinarias, ahora, origen),
      descripcion: c.descripcion,
      direccion: detalle.direccionesCampanas.get(c.id) ?? null,
    })),
    resenas: { promedio: reseñas.promedio, total: reseñas.total, recientes: reseñas.resenas },
  };
}
