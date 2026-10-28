/** Cuidado preventivo recomendado para una mascota (viene de una regla del repositorio). */
export interface ReglaDisponible {
  id: number;
  nombre: string;
  descripcion: string;
  importancia: string;
}

/** Una recomendación junto con si la mascota ya la tiene en su cronograma. */
export interface ReglaConEstado extends ReglaDisponible {
  enCronograma: boolean;
}
