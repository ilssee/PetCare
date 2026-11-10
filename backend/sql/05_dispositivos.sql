-- =====================================================================
-- PetCare · 05_dispositivos.sql
-- Celulares de cada usuario, para enviarles las notificaciones push (Firebase).
-- El "token" lo entrega Firebase a la app; el servidor lo guarda para saber a dónde enviar.
-- Se puede ejecutar varias veces: no borra nada.
-- =====================================================================

USE petcare;

CREATE TABLE IF NOT EXISTS dispositivo (
  id             INT UNSIGNED NOT NULL AUTO_INCREMENT,
  usuario_id     INT UNSIGNED NOT NULL,
  token          VARCHAR(512) NOT NULL COMMENT 'Código del celular que entrega Firebase',
  creado_en      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  -- Un celular tiene un solo token, y pertenece a un solo usuario a la vez.
  UNIQUE KEY uq_dispositivo_token (token),
  KEY ix_dispositivo_usuario (usuario_id),
  CONSTRAINT fk_dispositivo_usuario FOREIGN KEY (usuario_id)
    REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB;
