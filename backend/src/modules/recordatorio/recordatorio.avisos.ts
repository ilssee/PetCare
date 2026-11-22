import { Dia, diaDeAviso, diasEntre, diaDeLaSemana } from './recordatorio.dias';

/**
 * Decide qué avisos corresponde enviar un día determinado. No toca la base de datos ni envía nada:
 * recibe los cuidados pendientes y devuelve los avisos, para poder probar las reglas por separado.
 */

export type TipoAviso = 'anticipado' | 'pendiente';

/**
 * Lo que puede llevar una notificación: un recordatorio de cuidado, el aviso de nuevas recomendaciones (RF26)
 * o el aviso de una campaña o veterinaria nueva cerca del usuario (RF39 y RF40).
 */
export type TipoNotificacion = TipoAviso | 'recomendacion' | 'campana_cercana' | 'veterinaria_cercana';

/** Un cuidado pendiente de una mascota activa, con lo necesario para decidir si se avisa. */
export interface CuidadoPorAvisar {
  usuarioId: number;
  cuidadoId: number;
  cuidado: string;
  mascota: string;
  fechaRecurrente: string; // AAAA-MM-DD
  avisoAnticipado: boolean;
  especie?: string; // 'perro', 'gato' o 'conejo': la app la usa para elegir el ícono del aviso
}

/** Una notificación lista para enviar al celular del usuario. */
export interface Notificacion {
  usuarioId: number;
  cuidadoId: number; // 0 cuando no es de un cuidado concreto
  tipo: TipoNotificacion;
  titulo: string;
  cuerpo: string;
  /** Especie de la mascota del aviso (si lo hay): la app muestra el dibujo de esa especie como ícono. */
  especie?: string;
  /**
   * Solo en los avisos de lugares cercanos: 'privada' o 'municipal' (veterinaria) y 'activa' o 'proxima' (campaña).
   * La app lo usa para pintar el ícono con el mismo color que el marcador del mapa.
   */
  variante?: string;
  /** A qué pantalla lleva el aviso al tocarlo (solo en los avisos de lugares cercanos y de recomendaciones). */
  lugarId?: number; // campaña o veterinaria del aviso
  veterinariaId?: number; // perfil que se abre; en una campaña, solo si la organiza una veterinaria
  mascotaId?: number; // mascota de las recomendaciones nuevas
  reglasIds?: number[]; // recomendaciones nuevas: se abren desplegadas en la información preventiva
}

/** Un recordatorio de cuidado listo para enviar, con el nombre de la mascota y del cuidado. */
export interface Aviso extends Notificacion {
  cuidadoId: number;
  tipo: TipoAviso;
}

const NOMBRES_DIA: Record<Dia, string> = {
  lunes: 'lunes',
  martes: 'martes',
  miercoles: 'miércoles',
  jueves: 'jueves',
  viernes: 'viernes',
  sabado: 'sábado',
  domingo: 'domingo',
};

const MESES = [
  'enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio',
  'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre',
];

/** "2026-10-29" -> "miércoles 29 de octubre" */
export function fechaLarga(fecha: string): string {
  const [, mes, dia] = fecha.split('-').map(Number);
  return `${NOMBRES_DIA[diaDeLaSemana(fecha)]} ${dia} de ${MESES[mes - 1]}`;
}

const SEMANA: Dia[] = ['lunes', 'martes', 'miercoles', 'jueves', 'viernes', 'sabado', 'domingo'];

/**
 * Cuándo vence, dicho como se habla: "mañana", "este miércoles" (si cae en lo que queda de la semana,
 * de lunes a domingo) o "el próximo lunes" (si cae en la semana siguiente). El aviso anticipado se envía
 * como máximo 7 días antes, así que el día de la semana basta y no hace falta la fecha.
 */
function cuandoVence(hoy: string, vence: string, faltan: number): string {
  if (faltan === 1) return 'mañana';
  const nombre = NOMBRES_DIA[diaDeLaSemana(vence)];
  const sigueEnLaSemana = SEMANA.indexOf(diaDeLaSemana(hoy)) + faltan <= 6;
  return sigueEnLaSemana ? `este ${nombre}` : `el próximo ${nombre}`;
}

function avisoAnticipado(c: CuidadoPorAvisar, hoy: string): Aviso {
  const faltan = diasEntre(hoy, c.fechaRecurrente);
  const cuando = cuandoVence(hoy, c.fechaRecurrente, faltan);
  return {
    usuarioId: c.usuarioId,
    cuidadoId: c.cuidadoId,
    tipo: 'anticipado',
    titulo: 'Cuidado próximo a vencer',
    cuerpo: `${c.mascota}: ${c.cuidado} vence ${cuando}`,
    ...(c.especie ? { especie: c.especie } : {}),
  };
}

function avisoPendiente(c: CuidadoPorAvisar, hoy: string): Aviso {
  const vencido = diasEntre(c.fechaRecurrente, hoy);
  // El título dice la situación y el texto solo qué cuidado es y, si ya pasó la fecha, hace cuánto.
  const detalle = vencido === 0 ? '' : `, hace ${vencido} ${vencido === 1 ? 'día' : 'días'}`;
  return {
    usuarioId: c.usuarioId,
    cuidadoId: c.cuidadoId,
    tipo: 'pendiente',
    titulo: vencido === 0 ? 'Cuidado para hoy' : 'Cuidado vencido',
    cuerpo: `${c.mascota}: ${c.cuidado}${detalle}`,
    ...(c.especie ? { especie: c.especie } : {}),
  };
}

/**
 * Los avisos que corresponden a [hoy]:
 *  - RF21, anticipado: cuidados con aviso anticipado cuyo día de aviso es hoy. Ese día es el anterior al
 *    vencimiento o, si el usuario no lo tiene disponible, el día disponible más cercano anterior.
 *  - RF22, pendiente: cuidados cuya fecha ya llegó (hoy o antes) y siguen pendientes, todos los días
 *    hasta que se completen. Los días de disponibilidad no les aplican: son solo para el aviso anticipado.
 * [diasPorUsuario] trae los días elegidos por cada usuario; si falta, se usan todos.
 */
export function decidirAvisos(
  hoy: string,
  cuidados: CuidadoPorAvisar[],
  diasPorUsuario: ReadonlyMap<number, readonly Dia[]>,
): Aviso[] {
  const avisos: Aviso[] = [];
  for (const c of cuidados) {
    if (c.fechaRecurrente <= hoy) {
      avisos.push(avisoPendiente(c, hoy));
    } else if (c.avisoAnticipado) {
      const disponibles = new Set<Dia>(diasPorUsuario.get(c.usuarioId) ?? []);
      if (diaDeAviso(c.fechaRecurrente, disponibles) === hoy) avisos.push(avisoAnticipado(c, hoy));
    }
  }
  return avisos;
}
