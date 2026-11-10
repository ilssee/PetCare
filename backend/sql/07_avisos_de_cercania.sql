-- =====================================================================
-- PetCare · 07_avisos_de_cercania.sql
-- Para bases de datos que ya existían: agrega a campaña y veterinaria la fecha en que se avisó
-- a los usuarios cercanos (RF39 y RF40), igual que la regla (fecha_notificacion).
-- Una campaña o veterinaria con esa fecha vacía se considera nueva y se avisa una sola vez.
-- Se puede ejecutar varias veces: solo agrega la columna si todavía no existe.
-- (Las bases nuevas ya la traen desde 01_crear_tablas.sql.)
-- =====================================================================

USE petcare;

SET @existe = (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'campana' AND COLUMN_NAME = 'fecha_notificacion');
SET @sentencia = IF(@existe = 0,
  "ALTER TABLE campana ADD COLUMN fecha_notificacion DATE NULL COMMENT 'Vacía hasta que se avisa a los usuarios cercanos (RF39)'",
  'SELECT 1');
PREPARE ejecutar FROM @sentencia;
EXECUTE ejecutar;
DEALLOCATE PREPARE ejecutar;

SET @existe = (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'veterinaria' AND COLUMN_NAME = 'fecha_notificacion');
SET @sentencia = IF(@existe = 0,
  "ALTER TABLE veterinaria ADD COLUMN fecha_notificacion DATE NULL COMMENT 'Vacía hasta que se avisa a los usuarios cercanos (RF40)'",
  'SELECT 1');
PREPARE ejecutar FROM @sentencia;
EXECUTE ejecutar;
DEALLOCATE PREPARE ejecutar;
