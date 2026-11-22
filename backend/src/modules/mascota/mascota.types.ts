export type Sexo = 'macho' | 'hembra';
export type EstadoMascota = 'activa' | 'inactiva';

/** Mascota tal como se guarda (con el nombre de la especie ya resuelto). Raza, fecha y peso son opcionales. */
export interface MascotaRegistro {
  id: number;
  nombre: string;
  especie: string;
  raza: string | null;
  fechaNacimiento: string | null; // AAAA-MM-DD
  sexo: Sexo;
  peso: number | null; // kilogramos
  foto: string | null;
  estado: EstadoMascota;
  totalCuidados: number; // todos sus cuidados (pendientes y finalizados), sin contar sus actividades
}

/** Mascota que se devuelve a la app: incluye la edad calculada (dato derivado, no se guarda). */
export interface MascotaRespuesta extends MascotaRegistro {
  edadTexto: string | null; // null si la mascota no tiene fecha de nacimiento
}

/** Datos validados para crear o editar una mascota. */
export interface DatosMascota {
  nombre: string;
  raza: string | null;
  fechaNacimiento: string | null;
  sexo: Sexo;
  peso: number | null;
}
