package com.petcare.app.cuidado

/** Datos que se envían y reciben de la API (módulos regla y cuidado). */

/** Cuidado que el sistema recomienda para una mascota (viene de una regla preventiva). */
data class RecomendacionRespuesta(
    val id: Int,
    val nombre: String,
    val descripcion: String,
    val importancia: String,
)

/**
 * Cómo y cuándo se repite un cuidado. Lo define el usuario con su veterinario.
 * Modos de término: una_vez, varias_veces, hasta_fecha, indefinido.
 * Unidades de frecuencia: dias, semanas, meses, anios.
 */
data class ConfiguracionCuidado(
    val modoTermino: String,
    val frecuenciaValor: Int?,
    val frecuenciaUnidad: String?,
    val totalRepeticiones: Int?,
    val fechaLimite: String?,
    val fechaInicio: String, // AAAA-MM-DD: primera fecha del cuidado
    val avisoAnticipado: Boolean,
    val nota: String?,
) {
    fun paraRecomendacion(reglaId: Int) = CuidadoElegido(
        reglaId, modoTermino, frecuenciaValor, frecuenciaUnidad, totalRepeticiones,
        fechaLimite, fechaInicio, avisoAnticipado, nota,
    )
}

/** Un cuidado recomendado que el usuario eligió y configuró. Los campos vacíos no se envían. */
data class CuidadoElegido(
    val reglaId: Int,
    val modoTermino: String,
    val frecuenciaValor: Int?,
    val frecuenciaUnidad: String?,
    val totalRepeticiones: Int?,
    val fechaLimite: String?,
    val fechaInicio: String,
    val avisoAnticipado: Boolean,
    val nota: String?,
)

data class ActivarRecomendadosPeticion(val cuidados: List<CuidadoElegido>)

data class ActivarRecomendadosRespuesta(val mensaje: String)
