package com.petcare.app.mapa

import android.content.Context
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.petcare.app.R
import com.petcare.app.common.Enlaces
import com.petcare.app.common.CuadriculaDeOpciones
import com.petcare.app.mascota.Especies

/** Lo que muestra el detalle de una campaña (RF43), venga de su pantalla o de una tarjeta desplegada en el perfil. */
data class DatosCuerpoCampana(
    val descripcion: String,
    val especies: List<String>,
    val servicios: List<String>,
    val fechaInicio: String,
    val fechaFin: String,
    val horaApertura: String,
    val horaCierre: String,
    val direccion: String?,
    val lat: Double,
    val lng: Double,
)

fun CampanaRespuesta.comoCuerpo() = DatosCuerpoCampana(
    descripcion = descripcion.orEmpty(),
    especies = especies,
    servicios = servicios.map { it.nombre },
    fechaInicio = fechaInicio,
    fechaFin = fechaFin,
    horaApertura = horaApertura,
    horaCierre = horaCierre,
    direccion = direccion,
    lat = lat,
    lng = lng,
)

fun DetalleCampanaRespuesta.comoCuerpo() = DatosCuerpoCampana(
    descripcion = descripcion,
    especies = especies,
    servicios = servicios.map { it.nombre },
    fechaInicio = fechaInicio,
    fechaFin = fechaFin,
    horaApertura = horaApertura,
    horaCierre = horaCierre,
    direccion = direccion,
    lat = lat,
    lng = lng,
)

/**
 * Arma, con código, el cuerpo del detalle de una campaña: descripción, especies, servicios y "Cuándo y dónde".
 * Lo usan la pantalla de campañas de entidades externas y las tarjetas desplegables del perfil de la veterinaria.
 */
object CuerpoCampana {

    fun crear(contexto: Context, d: DatosCuerpoCampana): LinearLayout {
        val v = Ayuda(contexto)
        val cuerpo = LinearLayout(contexto).apply { orientation = LinearLayout.VERTICAL }

        if (d.descripcion.isNotBlank()) {
            cuerpo.addView(
                v.texto(d.descripcion, 14f, R.color.petcare_text).apply {
                    setBackgroundResource(R.drawable.bg_tarjeta_suave)
                    setPadding(v.dp(14), v.dp(12), v.dp(14), v.dp(12))
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                        .apply { topMargin = v.dp(4) }
                },
            )
        }

        val atendidas = listOf("perro", "gato", "conejo").filter { it in d.especies }
        if (atendidas.isNotEmpty()) {
            cuerpo.addView(v.titulo(R.string.campana_dirigida_a))
            val fila = LinearLayout(contexto).apply { orientation = LinearLayout.HORIZONTAL }
            atendidas.forEachIndexed { posicion, especie -> fila.addView(v.tarjetaDeEspecie(especie, primera = posicion == 0)) }
            cuerpo.addView(fila)
        }

        if (d.servicios.isNotEmpty()) {
            cuerpo.addView(v.titulo(R.string.campana_servicios))
            // Dos columnas iguales; si queda un servicio solo al final, ocupa toda la fila.
            cuerpo.addView(CuadriculaDeOpciones.crear(contexto, d.servicios.map { v.chipDeServicio(it) }, columnas = 2, separacion = v.dp(10)))
        }

        cuerpo.addView(v.titulo(R.string.campana_cuando_donde))
        cuerpo.addView(v.tarjetaCuandoYDonde(d))
        return cuerpo
    }

    /** Pequeñas piezas de pantalla con la misma tipografía y colores del perfil de la veterinaria. */
    private class Ayuda(private val contexto: Context) {
        fun dp(valor: Int) = (valor * contexto.resources.displayMetrics.density).toInt()
        private fun color(@ColorRes id: Int) = ContextCompat.getColor(contexto, id)
        private fun fuente(id: Int) = ResourcesCompat.getFont(contexto, id)

        fun texto(texto: String, tamano: Float, @ColorRes colorId: Int, negrita: Boolean = false) = TextView(contexto).apply {
            text = texto
            setTextColor(color(colorId))
            textSize = tamano
            typeface = fuente(if (negrita) R.font.poppins_semibold else R.font.poppins_regular)
        }

        fun titulo(@StringRes id: Int) = TextView(contexto).apply {
            setText(id)
            setTextColor(color(R.color.petcare_text_muted))
            textSize = 12f
            letterSpacing = 0.04f
            typeface = fuente(R.font.poppins_semibold)
            setPadding(0, dp(20), 0, dp(8))
        }

        fun tarjetaDeEspecie(especie: String, primera: Boolean): View {
            val tarjeta = LinearLayout(contexto).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setBackgroundResource(R.drawable.bg_icono_resultado)
                backgroundTintList = ColorStateList.valueOf(color(Especies.color(especie)))
                layoutParams = LinearLayout.LayoutParams(0, dp(100), 1f).apply { marginStart = if (primera) 0 else dp(8) }
            }
            tarjeta.addView(
                ImageView(contexto).apply {
                    setImageResource(Especies.ilustracion(especie))
                    layoutParams = LinearLayout.LayoutParams(dp(46), dp(46)).apply { gravity = Gravity.CENTER_HORIZONTAL }
                },
            )
            tarjeta.addView(
                TextView(contexto).apply {
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    gravity = Gravity.CENTER_HORIZONTAL
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                    setText(Especies.nombre(especie))
                    setTextColor(color(R.color.petcare_text))
                    textSize = 14f
                    typeface = fuente(R.font.poppins_semibold)
                    setPadding(0, dp(6), 0, 0)
                },
            )
            return tarjeta
        }

        fun chipDeServicio(nombre: String) = TextView(contexto).apply {
            text = nombre
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_servicio_pildora)
            setTextColor(color(R.color.petcare_primary_dark))
            textSize = 13f
            typeface = fuente(R.font.poppins_semibold)
            setPadding(dp(12), dp(13), dp(12), dp(13))
        }

        private fun iconoEnCuadro(@DrawableRes icono: Int) = ImageView(contexto).apply {
            setImageResource(icono)
            setBackgroundResource(R.drawable.bg_icono_info)
            imageTintList = ColorStateList.valueOf(color(R.color.petcare_primary_dark))
            setPadding(dp(10), dp(10), dp(10), dp(10))
            layoutParams = LinearLayout.LayoutParams(dp(42), dp(42))
        }

        private fun fila(@DrawableRes icono: Int, vararg lineas: View): View {
            val fila = LinearLayout(contexto).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(6), 0, dp(6))
            }
            fila.addView(iconoEnCuadro(icono))
            val columna = LinearLayout(contexto).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(12) }
            }
            for (linea in lineas) columna.addView(linea)
            fila.addView(columna)
            return fila
        }

        /** La dirección y, en la misma línea a su derecha, "Cómo llegar" (abre la ruta en Google Maps). */
        fun lineaDeDireccion(d: DatosCuerpoCampana): View {
            val linea = LinearLayout(contexto).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.TOP
            }
            val direccion = d.direccion?.takeIf { it.isNotBlank() } ?: contexto.getString(R.string.campana_sin_direccion)
            linea.addView(
                texto(direccion, 14f, R.color.petcare_text, negrita = true).apply {
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                },
            )
            linea.addView(
                TextView(contexto).apply {
                    setText(R.string.como_llegar)
                    setTextColor(color(R.color.petcare_primary_dark))
                    textSize = 14f
                    typeface = fuente(R.font.poppins_semibold)
                    setPadding(dp(12), 0, 0, 0)
                    setOnClickListener { Enlaces.comoLlegar(contexto, d.lat, d.lng) }
                },
            )
            return linea
        }

        fun tarjetaCuandoYDonde(d: DatosCuerpoCampana): View {
            val tarjeta = LinearLayout(contexto).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_tarjeta_suave)
                setPadding(dp(14), dp(8), dp(14), dp(8))
            }
            val fechas = if (d.fechaInicio == d.fechaFin) {
                texto(FormatosMapa.fechaConAnio(d.fechaInicio), 14f, R.color.petcare_text, negrita = true)
            } else {
                texto(
                    contexto.getString(
                        R.string.campana_fechas_del_al,
                        FormatosMapa.fechaConAnio(d.fechaInicio),
                        FormatosMapa.fechaConAnio(d.fechaFin),
                    ),
                    14f,
                    R.color.petcare_text,
                    negrita = true,
                )
            }
            tarjeta.addView(fila(R.drawable.ic_calendario, texto(contexto.getString(R.string.campana_fechas), 12f, R.color.petcare_text_muted), fechas))
            tarjeta.addView(
                fila(
                    R.drawable.ic_reloj,
                    texto(contexto.getString(R.string.campana_horario_diario), 12f, R.color.petcare_text_muted),
                    texto("${FormatosMapa.hora(d.horaApertura)} – ${FormatosMapa.hora(d.horaCierre)}", 14f, R.color.petcare_text, negrita = true),
                    texto(contexto.getString(R.string.campana_mismo_horario), 12f, R.color.petcare_text_muted),
                ),
            )
            // La ubicación siempre se muestra: aunque no tenga dirección escrita, se puede abrir la ruta.
            tarjeta.addView(
                fila(
                    R.drawable.ic_mapa,
                    texto(contexto.getString(R.string.campana_ubicacion), 12f, R.color.petcare_text_muted),
                    lineaDeDireccion(d),
                ),
            )
            return tarjeta
        }
    }
}
