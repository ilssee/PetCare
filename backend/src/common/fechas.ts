/** Utilidades de fechas compartidas por los módulos. Las fechas viajan como texto "AAAA-MM-DD". */

const FORMATO_FECHA = /^\d{4}-\d{2}-\d{2}$/;

export function fechaDeHoy(): string {
  const hoy = new Date();
  const mes = String(hoy.getMonth() + 1).padStart(2, '0');
  const dia = String(hoy.getDate()).padStart(2, '0');
  return `${hoy.getFullYear()}-${mes}-${dia}`;
}

function aTexto(fecha: Date): string {
  const mes = String(fecha.getUTCMonth() + 1).padStart(2, '0');
  const dia = String(fecha.getUTCDate()).padStart(2, '0');
  return `${fecha.getUTCFullYear()}-${mes}-${dia}`;
}

/**
 * Suma una frecuencia (dias, semanas, meses o anios) a una fecha AAAA-MM-DD.
 * Al sumar meses o años, si el día no existe en el mes de destino se usa el último día
 * (por ejemplo, 31 de enero + 1 mes = 28 de febrero).
 */
export function sumarFrecuencia(fecha: string, valor: number, unidad: string): string {
  const [anio, mes, dia] = fecha.split('-').map(Number);

  if (unidad === 'dias' || unidad === 'semanas') {
    const dias = unidad === 'semanas' ? valor * 7 : valor;
    return aTexto(new Date(Date.UTC(anio, mes - 1, dia + dias)));
  }

  const meses = mes - 1 + (unidad === 'anios' ? valor * 12 : valor);
  const anioDestino = anio + Math.floor(meses / 12);
  const mesDestino = ((meses % 12) + 12) % 12;
  const ultimoDia = new Date(Date.UTC(anioDestino, mesDestino + 1, 0)).getUTCDate();
  return aTexto(new Date(Date.UTC(anioDestino, mesDestino, Math.min(dia, ultimoDia))));
}

/** Verifica el formato y que la fecha exista (rechaza, por ejemplo, 2026-02-31). */
export function esFechaReal(texto: string): boolean {
  if (!FORMATO_FECHA.test(texto)) return false;
  const [anio, mes, dia] = texto.split('-').map(Number);
  const fecha = new Date(Date.UTC(anio, mes - 1, dia));
  return fecha.getUTCFullYear() === anio && fecha.getUTCMonth() === mes - 1 && fecha.getUTCDate() === dia;
}
