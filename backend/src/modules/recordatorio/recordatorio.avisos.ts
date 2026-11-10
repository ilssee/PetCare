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
}

/** Una notificación lista para enviar al celular del usuario. */
export interface Notificacion {
  usuarioId: number;
  cuidadoId: number; // 0 cuando no es de un cuidado concreto
  tipo: TipoNotificacion;
  titulo: string;
  cuerpo: string;
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

function avisoAnticipado(c: CuidadoPorAvisar, hoy: string): Aviso {
  const faltan = diasEntre(hoy, c.fechaRecurrente);
  const cuando = faltan === 1 ? 'mañana' : `el ${fechaLarga(c.fechaRecurrente)}`;
  return {
    usuarioId: c.usuarioId,
    cuidadoId: c.cuidadoId,
    tipo: 'anticipado',
    titulo: 'Cuidado próximo a vencer',
    cuerpo: `${c.mascota}: ${c.cuidado} vence ${cuando}`,
  };
}

function avisoPendiente(c: CuidadoPorAvisar, hoy: string): Aviso {
  const vencido = diasEntre(c.fechaRecurrente, hoy);
  const estado = vencido === 0 ? 'toca hoy' : `venció hace ${vencido} ${vencido === 1 ? 'día' : 'días'}`;
  return {
    usuarioId: c.usuarioId,
    cuidadoId: c.cuidadoId,
    tipo: 'pendiente',
    titulo: 'Cuidado pendiente',
    cuerpo: `${c.mascota}: ${c.cuidado} ${estado}`,
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
