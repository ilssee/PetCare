import express from 'express';
import { pool } from './config/database';
import { manejarErrores } from './common/middlewares/error-handler';
import { carpetaUploads } from './config/uploads';
import { usuarioRoutes } from './modules/usuario/usuario.routes';
import { mascotaRoutes } from './modules/mascota/mascota.routes';

/** Aplicación Express. Aquí se registran las rutas de cada módulo. */
export const app = express();

app.use(express.json());

// Ruta de comprobación: confirma que el servidor está arriba.
app.get('/salud', (_req, res) => {
  res.json({ estado: 'ok' });
});

// Prueba de conexión: confirma que la API puede consultar MySQL.
app.get('/salud/bd', async (_req, res) => {
  try {
    await pool.query('SELECT 1');
    res.json({ estado: 'ok', baseDeDatos: 'conectada' });
  } catch (error) {
    console.error('Error al conectar con MySQL:', error);
    res.status(500).json({ estado: 'error', baseDeDatos: 'sin conexión' });
  }
});

// Fotos subidas: públicas, pero con nombres aleatorios imposibles de adivinar.
app.use('/uploads', express.static(carpetaUploads));

// Rutas de los módulos
app.use('/usuarios', usuarioRoutes);
app.use('/mascotas', mascotaRoutes);

// Debe ir al final: atrapa los errores de todas las rutas anteriores.
app.use(manejarErrores);
