# Backend · Sistema PetCare

API REST con Node.js, Express, TypeScript y MySQL.

## Estructura actual
```
src/
  config/     lectura del .env y conexión a MySQL
  common/     código compartido entre módulos (errores, middlewares)
  app.ts      aplicación Express y registro de rutas
  server.ts   arranque del servidor
```
Los módulos (`usuario`, `mascota`, `cuidado`, `regla`, `organizador`, `resena`, `campana`, `notificacion`) se crean dentro de `src/modules/` a medida que se programan. Cada uno tiene sus capas: rutas, controlador, servicio y repositorio.

## Cómo ejecutarlo
1. Instalar dependencias: `npm install`
2. Copiar `.env.example` a `.env` y completar los datos de MySQL.
3. Iniciar en modo desarrollo: `npm run dev`
4. Probar en el navegador: http://localhost:3000/salud

## Scripts
| Comando | Qué hace |
|---|---|
| `npm run dev` | Inicia el servidor y lo reinicia al guardar cambios |
| `npm run build` | Compila TypeScript a la carpeta `dist/` |
| `npm start` | Ejecuta la versión compilada |
