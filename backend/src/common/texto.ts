/** Un texto opcional que llega vacío se guarda como null. */
export function textoOpcional(valor: unknown): string | null {
  const texto = String(valor ?? '').trim();
  return texto === '' ? null : texto;
}
