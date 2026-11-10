/** Los días de la semana de los recordatorios (RF20), tal como se guardan en la base de datos. */
export const DIAS = ['lunes', 'martes', 'miercoles', 'jueves', 'viernes', 'sabado', 'domingo'] as const;
export type Dia = (typeof DIAS)[number];

// getUTCDay() devuelve 0 para domingo; se reordena para que lunes sea el primero.
const POR_NUMERO: Dia[] = ['domingo', 'lunes', 'martes', 'miercoles', 'jueves', 'viernes', 'sabado'];

function aFecha(texto: string): Date {
  const [anio, mes, dia] = texto.split('-').map(Number);
  return new Date(Date.UTC(anio, mes - 1, dia));
}

/** Día de la semana de una fecha AAAA-MM-DD. */
export function diaDeLaSemana(fecha: string): Dia {
  return POR_NUMERO[aFecha(fecha).getUTCDay()];
}

/** Suma (o resta, con un número negativo) días a una fecha AAAA-MM-DD. */
export function sumarDias(fecha: string, dias: number): string {
  const resultado = aFecha(fecha);
  resultado.setUTCDate(resultado.getUTCDate() + dias);
  const mes = String(resultado.getUTCMonth() + 1).padStart(2, '0');
  const dia = String(resultado.getUTCDate()).padStart(2, '0');
  return `${resultado.getUTCFullYear()}-${mes}-${dia}`;
}

/** Cuántos días hay desde [desde] hasta [hasta] (positivo si [hasta] es posterior). */
export function diasEntre(desde: string, hasta: string): number {
  return Math.round((aFecha(hasta).getTime() - aFecha(desde).getTime()) / 86_400_000);
}

/**
 * Día en que se envía el aviso anticipado de un cuidado que vence en [vence] (RF21):
 * el día anterior al vencimiento si el usuario lo tiene disponible; si no, el día disponible
 * más cercano anterior a esa fecha.
 */
export function diaDeAviso(vence: string, disponibles: ReadonlySet<Dia>): string {
  // Sin ninguno configurado se usan todos los días (RF20), así que siempre se encuentra uno.
  const todos = disponibles.size === 0;
  let dia = sumarDias(vence, -1);
  for (let intento = 0; intento < 7; intento++) {
    if (todos || disponibles.has(diaDeLaSemana(dia))) return dia;
    dia = sumarDias(dia, -1);
  }
  return dia;
}
