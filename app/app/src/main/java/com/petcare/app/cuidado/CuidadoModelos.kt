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
    val fechaInicio: String, // AAAA-MM-DD: primera (o próxima) fecha del cuidado
    val avisoAnticipado: Boolean,
    val nota: String?,
) {
    fun paraRecomendacion(reglaId: Int) = CuidadoElegido(
        reglaId, modoTermino, frecuenciaValor, frecuenciaUnidad, totalRepeticiones,
        fechaLimite, fechaInicio, avisoAnticipado, nota,
    )

    /** Nombre y descripción solo se envían en los cuidados personalizados; en los recomendados están bloqueados. */
    fun paraGuardar(nombre: String?, descripcion: String?) = CuidadoPeticion(
        nombre, descripcion, modoTermino, frecuenciaValor, frecuenciaUnidad, totalRepeticiones,
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

/** Para agregar un cuidado personalizado o editar uno existente. */
data class CuidadoPeticion(
    val nombre: String?,
    val descripcion: String?,
    val modoTermino: String,
    val frecuenciaValor: Int?,
    val frecuenciaUnidad: String?,
    val totalRepeticiones: Int?,
    val fechaLimite: String?,
    val fechaInicio: String,
    val avisoAnticipado: Boolean,
    val nota: String?,
)

/** Una ocasión en que se realizó el cuidado (historial). */
data class ActividadRespuesta(
    val id: Int,
    val cuidadoId: Int,
    val fechaPlanificada: String,
    val fechaRealizacion: String,
    val estado: String, // completada o anulada
    val nota: String?,
)

/** Un cuidado de una mascota, con su historial de actividades (de la más reciente a la más antigua). */
data class CuidadoRespuesta(
    val id: Int,
    val mascotaId: Int,
    val reglaId: Int?, // null si es un cuidado personalizado
    val nombre: String,
    val descripcion: String,
    val importancia: String?, // solo los recomendados la tienen
    val nota: String?,
    val modoTermino: String,
    val frecuenciaValor: Int?,
    val frecuenciaUnidad: String?,
    val totalRepeticiones: Int?,
    val fechaLimite: String?,
    val fechaRecurrente: String, // próxima fecha del cuidado
    val avisoAnticipado: Boolean,
    val estado: String, // pendiente o finalizado
    val actividades: List<ActividadRespuesta>,
    val realizadas: Int,
) {
    val esRecomendado: Boolean get() = reglaId != null
    val estaFinalizado: Boolean get() = estado == "finalizado"
}

/** Cuántos cuidados tiene la mascota en cada estado (no depende del filtro elegido). */
data class ConteoCuidados(val todos: Int, val pendientes: Int, val finalizados: Int)

data class ListaCuidadosRespuesta(val conteos: ConteoCuidados, val cuidados: List<CuidadoRespuesta>)

data class GuardarCuidadoRespuesta(val mensaje: String, val cuidado: CuidadoRespuesta)

/** Cuidados de todas las mascotas activas del usuario (cronograma). */
data class CronogramaRespuesta(val cuidados: List<CuidadoRespuesta>)
