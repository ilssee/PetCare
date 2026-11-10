package com.petcare.app.recordatorio

/** Datos que se envían y reciben de la API de recordatorios. */

/** Días de la semana (lunes, martes, miercoles, jueves, viernes, sabado, domingo) en que se avisa. */
data class DiasRecordatorio(val dias: List<String>)

data class GuardarDiasPeticion(val dias: List<String>)

data class GuardarDiasRespuesta(val mensaje: String, val dias: List<String>)
