import { Router } from 'express';
import { exigirSesion } from '../../common/middlewares/auth.middleware';
import * as controlador from './recordatorio.controller';

/** Configuración de recordatorios del usuario, montada en /recordatorios (RF20). Exige sesión iniciada. */
export const recordatorioRoutes = Router();

recordatorioRoutes.use(exigirSesion);
recordatorioRoutes.get('/', controlador.obtener);
recordatorioRoutes.put('/', controlador.guardar);
