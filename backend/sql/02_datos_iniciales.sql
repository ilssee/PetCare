-- =====================================================================
-- PetCare · 02_datos_iniciales.sql
-- Datos que el sistema necesita para funcionar (no son datos de usuarios).
-- Se puede ejecutar varias veces: INSERT IGNORE no duplica lo que ya existe.
-- =====================================================================

USE petcare;

-- Especies atendidas por el sistema
INSERT IGNORE INTO especie (nombre) VALUES ('perro'), ('gato'), ('conejo');

-- Pendiente (siguientes tareas): reglas preventivas, categorías y servicios.
