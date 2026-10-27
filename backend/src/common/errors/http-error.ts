/**
 * Error con código HTTP. Los servicios lo lanzan cuando algo no es válido
 * (por ejemplo, correo repetido) y el manejador de errores lo convierte en respuesta.
 */
export class HttpError extends Error {
  constructor(
    public readonly estado: number,
    mensaje: string,
  ) {
    super(mensaje);
  }
}
