package com.petcare.app.mapa

import android.content.Context
import android.widget.ImageView
import android.widget.LinearLayout
import com.petcare.app.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Textos y estrellas que comparten el mapa, el perfil de la veterinaria y las reseñas. */
object FormatosMapa {

    /** "9:00" a partir de "09:00:00". */
    fun hora(texto: String) = "${texto.take(2).toIntOrNull() ?: 0}:${texto.substring(3, 5)}"

    /** "9:00–19:00", "9:00–13:00, 15:00–19:00" o "24 horas". Sin franjas, "Cerrado". */
    fun rangoDeFranjas(contexto: Context, franjas: List<FranjaHoy>): String {
        if (franjas.isEmpty()) return contexto.getString(R.string.mapa_cerrado)
        val atiendeTodoElDia = franjas.size == 1 && franjas[0].apertura.startsWith("00:00") && franjas[0].cierre >= "23:59"
        if (atiendeTodoElDia) return contexto.getString(R.string.mapa_24_horas)
        return franjas.joinToString(", ") { "${hora(it.apertura)}–${hora(it.cierre)}" }
    }

    /** "Hoy · 9:00–19:00" u "Hoy cerrado". */
    fun horarioDeHoy(contexto: Context, franjas: List<FranjaHoy>): String =
        if (franjas.isEmpty()) contexto.getString(R.string.mapa_hoy_cerrado)
        else contexto.getString(R.string.mapa_hoy_horario, rangoDeFranjas(contexto, franjas))

    /** "4 oct" a partir de "2026-10-04". */
    fun fechaCorta(texto: String): String = runCatching {
        LocalDate.parse(texto).format(DateTimeFormatter.ofPattern("d MMM", Locale("es", "PE"))).replace(".", "")
    }.getOrDefault(texto)

    /** "4 oct 2026" a partir de "2026-10-04". */
    fun fechaConAnio(texto: String): String = runCatching {
        LocalDate.parse(texto).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es", "PE"))).replace(".", "")
    }.getOrDefault(texto)

    /** "4.7" para mostrar una calificación. */
    fun calificacion(promedio: Double): String = "%.1f".format(Locale.US, promedio)

    /** Pinta cinco estrellas: las primeras [llenas] en dorado y el resto en gris. */
    fun pintarEstrellas(contenedor: LinearLayout, llenas: Int, tamanoDp: Int) {
        contenedor.removeAllViews()
        val tamano = (tamanoDp * contenedor.resources.displayMetrics.density).toInt()
        for (i in 1..5) {
            contenedor.addView(
                ImageView(contenedor.context).apply {
                    setImageResource(if (i <= llenas) R.drawable.ic_estrella_llena else R.drawable.ic_estrella_vacia)
                    layoutParams = LinearLayout.LayoutParams(tamano, tamano)
                },
            )
        }
    }

    /** Las estrellas de un promedio se redondean al entero más cercano, como en el listado del mapa. */
    fun estrellasDe(promedio: Double?): Int = promedio?.let { Math.round(it).toInt() } ?: 0
}
