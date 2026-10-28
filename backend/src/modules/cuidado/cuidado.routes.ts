import { Router } from 'express';
import { exigirSesion } from '../../common/middlewares/auth.middleware';
import * as controlador from './cuidado.controller';

/** Rutas de cuidados, montadas en /mascotas porque cada cuidado pertenece a una mascota. */
export const cuidadoRoutes = Router();

cuidadoRoutes.post('/:id/cuidados/recomendados', exigirSesion, controlador.activarRecomendados);
