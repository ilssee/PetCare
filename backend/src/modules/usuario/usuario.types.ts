/** Usuario tal como se guarda en la base de datos. */
export interface UsuarioRegistro {
  id: number;
  nombre: string;
  correo: string;
  contrasena: string; // hash, nunca la clave en texto plano
}

/** Usuario que se devuelve a la app: sin la contraseña. */
export interface UsuarioPublico {
  id: number;
  nombre: string;
  correo: string;
}

/** Datos que llegan en el registro. */
export interface DatosRegistro {
  nombre: string;
  correo: string;
  contrasena: string;
}

/** Datos que llegan en el inicio de sesión. */
export interface DatosLogin {
  correo: string;
  contrasena: string;
}
