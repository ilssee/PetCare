/** Utilidades de fechas compartidas por los módulos. Las fechas viajan como texto "AAAA-MM-DD". */

const FORMATO_FECHA = /^\d{4}-\d{2}-\d{2}$/;

export function fechaDeHoy(): string {
  const hoy = new Date();
  const mes = String(hoy.getMonth() + 1).padStart(2, '0');
  const dia = String(hoy.getDate()).padStart(2, '0');
  return `${hoy.getFullYear()}-${mes}-${dia}`;
}

/** Verifica el formato y que la fecha exista (rechaza, por ejemplo, 2026-02-31). */
export function esFechaReal(texto: string): boolean {
  if (!FORMATO_FECHA.test(texto)) return false;
  const [anio, mes, dia] = texto.split('-').map(Number);
  const fecha = new Date(Date.UTC(anio, mes - 1, dia));
  return fecha.getUTCFullYear() === anio && fecha.getUTCMonth() === mes - 1 && fecha.getUTCDate() === dia;
}
