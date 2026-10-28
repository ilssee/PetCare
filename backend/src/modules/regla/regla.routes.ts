import { Router } from 'express';
import { exigirSesion } from '../../common/middlewares/auth.middleware';
import * as controlador from './regla.controller';

/** Rutas de reglas, montadas en /mascotas porque siempre se consultan para una mascota. */
export const reglaRoutes = Router();

reglaRoutes.get('/:id/recomendaciones', exigirSesion, controlador.listarDisponibles);
