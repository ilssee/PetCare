package com.petcare.app.cuidado

import android.content.Context
import com.petcare.app.R
import com.petcare.app.mascota.comoFechaLegible
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Textos de un cuidado tal como se muestran en las tarjetas. */

/** "2026-10-10" -> "10 oct" */
fun String.comoFechaCorta(): String = runCatching {
    LocalDate.parse(this).format(DateTimeFormatter.ofPattern("d MMM", Locale("es", "PE"))).replace(".", "")
}.getOrDefault(this)

/** Días que lleva vencido el cuidado (0 si no está vencido). Solo vencen los pendientes. */
fun CuidadoRespuesta.diasVencido(hoy: LocalDate = LocalDate.now()): Long {
    if (estaFinalizado) return 0
    val fecha = runCatching { LocalDate.parse(fechaRecurrente) }.getOrNull() ?: return 0
    return maxOf(0, ChronoUnit.DAYS.between(fecha, hoy))
}

/** "Aún sin realizar" o "Realizado 2 veces". */
fun CuidadoRespuesta.textoRealizado(contexto: Context): String =
    if (realizadas == 0) contexto.getString(R.string.cuidado_sin_realizar)
    else contexto.resources.getQuantityString(R.plurals.cuidado_realizado, realizadas, realizadas)

/** "Cada mes", "Cada 12 meses" o "Una vez". */
fun CuidadoRespuesta.textoFrecuencia(contexto: Context): String {
    val valor = frecuenciaValor
    val unidad = frecuenciaUnidad
    if (modoTermino == "una_vez" || valor == null || unidad == null) return contexto.getString(R.string.modo_una_vez)
    if (valor == 1) {
        return contexto.getString(
            when (unidad) {
                "dias" -> R.string.cada_dia
                "semanas" -> R.string.cada_semana
                "meses" -> R.string.cada_mes
                else -> R.string.cada_anio
            },
        )
    }
    val nombreUnidad = contexto.getString(
        when (unidad) {
            "dias" -> R.string.unidad_dias
            "semanas" -> R.string.unidad_semanas
            "meses" -> R.string.unidad_meses
            else -> R.string.unidad_anios
        },
    )
    return contexto.getString(R.string.cada_n, valor, nombreUnidad)
}

/** "Indefinido", "Una vez", "6 veces" o "Hasta 5 feb 2030". */
fun CuidadoRespuesta.textoDuracion(contexto: Context): String = when (modoTermino) {
    "varias_veces" -> {
        val veces = totalRepeticiones ?: 0
        contexto.resources.getQuantityString(R.plurals.cuidado_veces, veces, veces)
    }
    "hasta_fecha" -> contexto.getString(R.string.cuidado_hasta, fechaLimite?.comoFechaLegible() ?: "")
    "indefinido" -> contexto.getString(R.string.modo_indefinido)
    else -> contexto.getString(R.string.modo_una_vez)
}

/** La duración escrita como frase, para los detalles del cuidado: "Se repite sin fecha final". */
fun CuidadoRespuesta.textoRepeticion(contexto: Context): String = when (modoTermino) {
    "varias_veces" -> {
        val veces = totalRepeticiones ?: 0
        contexto.resources.getQuantityString(R.plurals.cuidado_repite_veces, veces, veces)
    }
    "hasta_fecha" -> contexto.getString(R.string.cuidado_repite_hasta, fechaLimite?.comoFechaLegible() ?: "")
    "indefinido" -> contexto.getString(R.string.cuidado_repite_indefinido)
    else -> contexto.getString(R.string.cuidado_repite_una_vez)
}

/**
 * Próxima fecha del cuidado si se calcula a partir de [base] (planificada o de realización).
 * Es la misma cuenta que hace el servidor; sirve para mostrar el resultado antes de elegir.
 */
fun CuidadoRespuesta.proximaFechaDesde(base: LocalDate): LocalDate? {
    val valor = frecuenciaValor ?: return null
    return when (frecuenciaUnidad) {
        "dias" -> base.plusDays(valor.toLong())
        "semanas" -> base.plusWeeks(valor.toLong())
        "meses" -> base.plusMonths(valor.toLong())
        "anios" -> base.plusYears(valor.toLong())
        else -> null
    }
}
