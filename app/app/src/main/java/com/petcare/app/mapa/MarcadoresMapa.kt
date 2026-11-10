package com.petcare.app.mapa

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.petcare.app.R

/**
 * Marcadores del mapa, con la forma del prototipo de Figma: un globo de borde blanco con una punta abajo.
 *  - veterinaria: globo redondo azul (privada) o rosado (municipal), con la casa y la huella;
 *  - campaña: globo cuadrado de esquinas redondeadas, verde (Activa) o naranja (Próxima), con la cruz y la huella;
 *  - veterinaria con campaña en su misma ubicación: el globo de la veterinaria con una insignia de la campaña.
 * Los íconos blancos (casa con huella y cruz con huella) son los mismos del prototipo.
 */
object MarcadoresMapa {

    @ColorRes
    fun colorDeVeterinaria(tipo: String) = if (tipo == "municipal") R.color.mapa_municipal else R.color.mapa_privada

    @ColorRes
    fun colorDeCampana(estado: String) = if (estado == "activa") R.color.mapa_activa else R.color.mapa_proxima

    /** El ancla de estos marcadores es la punta del globo (abajo al centro). */
    fun veterinaria(contexto: Context, tipo: String, campanasAqui: List<CampanaAqui>): Drawable {
        val insignia = if (campanasAqui.isEmpty()) {
            null
        } else {
            // Si alguna de las campañas está Activa, la insignia es verde; si todas son Próximas, naranja.
            colorDeCampana(if (campanasAqui.any { it.estado == "activa" }) "activa" else "proxima")
        }
        return globo(contexto, colorDeVeterinaria(tipo), R.drawable.ic_mapa_veterinaria, redondo = true, colorInsignia = insignia)
    }

    fun campana(contexto: Context, estado: String): Drawable =
        globo(contexto, colorDeCampana(estado), R.drawable.ic_mapa_campana, redondo = false, colorInsignia = null)

    /** Punto azul con aro blanco: la ubicación del usuario (el ancla es el centro). */
    fun ubicacionActual(contexto: Context): Drawable {
        val d = contexto.resources.displayMetrics.density
        val tamano = (22 * d).toInt()
        val bitmap = Bitmap.createBitmap(tamano, tamano, Bitmap.Config.ARGB_8888)
        val lienzo = Canvas(bitmap)
        val pincel = Paint(Paint.ANTI_ALIAS_FLAG)
        val centro = tamano / 2f
        pincel.color = ContextCompat.getColor(contexto, R.color.mapa_ubicacion) and 0x00FFFFFF or 0x40000000
        lienzo.drawCircle(centro, centro, centro, pincel) // halo translúcido
        pincel.color = ContextCompat.getColor(contexto, R.color.white)
        lienzo.drawCircle(centro, centro, 7 * d, pincel)
        pincel.color = ContextCompat.getColor(contexto, R.color.mapa_ubicacion)
        lienzo.drawCircle(centro, centro, 5 * d, pincel)
        return BitmapDrawable(contexto.resources, bitmap)
    }

    /**
     * Marcador de la ubicación de referencia (pantalla Avisos cerca de ti): una gota verde azulada con un punto
     * blanco. El ancla es la punta de abajo. Es la que el usuario arrastra para ajustar el lugar exacto.
     */
    fun referencia(contexto: Context): Drawable {
        val d = contexto.resources.displayMetrics.density
        val radio = 15 * d
        val ancho = 2 * radio + 8 * d
        val alto = 2 * radio + 14 * d
        val bitmap = Bitmap.createBitmap(ancho.toInt(), alto.toInt(), Bitmap.Config.ARGB_8888)
        val lienzo = Canvas(bitmap)
        val pincel = Paint(Paint.ANTI_ALIAS_FLAG)
        val centroX = ancho / 2
        val centroY = 4 * d + radio
        val teal = ContextCompat.getColor(contexto, R.color.petcare_primary)
        val blanco = ContextCompat.getColor(contexto, R.color.white)

        fun gota(r: Float): Path = Path().apply {
            addCircle(centroX, centroY, r, Path.Direction.CW)
            // La punta: un triángulo que sale de la parte baja del círculo.
            moveTo(centroX - r * 0.62f, centroY + r * 0.78f)
            lineTo(centroX, centroY + r + 9 * d)
            lineTo(centroX + r * 0.62f, centroY + r * 0.78f)
            close()
        }
        pincel.setShadowLayer(3 * d, 0f, 2 * d, 0x55000000)
        pincel.color = blanco
        lienzo.drawPath(gota(radio), pincel)
        pincel.clearShadowLayer()
        pincel.color = teal
        lienzo.drawPath(gota(radio - 2.5f * d), pincel)
        pincel.color = blanco
        lienzo.drawCircle(centroX, centroY, 5 * d, pincel)
        return BitmapDrawable(contexto.resources, bitmap)
    }

    private fun globo(
        contexto: Context,
        @ColorRes color: Int,
        @DrawableRes glifo: Int,
        redondo: Boolean,
        @ColorRes colorInsignia: Int?,
    ): Drawable {
        val d = contexto.resources.displayMetrics.density
        val cuerpo = 42 * d // lado del globo
        val borde = 3 * d
        val margen = 8 * d // espacio para la sombra y la insignia
        val punta = 6 * d // mitad de la diagonal del rombo de abajo

        val ancho = cuerpo + 2 * margen
        val alto = margen + cuerpo + punta * 0.8f + 4 * d
        val bitmap = Bitmap.createBitmap(ancho.toInt(), alto.toInt(), Bitmap.Config.ARGB_8888)
        val lienzo = Canvas(bitmap)
        val pincel = Paint(Paint.ANTI_ALIAS_FLAG)

        val caja = RectF(margen, margen, margen + cuerpo, margen + cuerpo)
        val radio = if (redondo) cuerpo / 2 else 13 * d
        val centroX = ancho / 2

        // Sombra suave del conjunto
        pincel.setShadowLayer(4 * d, 0f, 2 * d, 0x55000000)
        pincel.color = 0xFFFFFFFF.toInt()
        lienzo.drawRoundRect(caja, radio, radio, pincel)
        pincel.clearShadowLayer()

        // Punta: rombo blanco y, encima, uno más chico del color del globo
        val centroPuntaY = margen + cuerpo - 2 * d
        fun rombo(mitad: Float) = Path().apply {
            moveTo(centroX, centroPuntaY - mitad)
            lineTo(centroX + mitad, centroPuntaY)
            lineTo(centroX, centroPuntaY + mitad)
            lineTo(centroX - mitad, centroPuntaY)
            close()
        }
        pincel.color = ContextCompat.getColor(contexto, R.color.white)
        lienzo.drawPath(rombo(punta + 1.5f * d), pincel)
        pincel.color = ContextCompat.getColor(contexto, color)
        lienzo.drawPath(rombo(punta - 1f * d), pincel)

        // Globo: borde blanco y relleno de color
        pincel.color = ContextCompat.getColor(contexto, R.color.white)
        lienzo.drawRoundRect(caja, radio, radio, pincel)
        val interior = RectF(caja.left + borde, caja.top + borde, caja.right - borde, caja.bottom - borde)
        pincel.color = ContextCompat.getColor(contexto, color)
        lienzo.drawRoundRect(interior, maxOf(radio - borde, 0f), maxOf(radio - borde, 0f), pincel)

        // Ícono blanco centrado
        val mitadGlifo = cuerpo * 0.30f
        ponerGlifo(contexto, glifo, caja.centerX() - mitadGlifo, caja.centerY() - mitadGlifo, caja.centerX() + mitadGlifo, caja.centerY() + mitadGlifo, lienzo)

        // Insignia de campaña, arriba a la izquierda
        if (colorInsignia != null) {
            val cx = caja.left + 3 * d
            val cy = caja.top + 3 * d
            val r = 10 * d
            pincel.color = ContextCompat.getColor(contexto, R.color.white)
            lienzo.drawCircle(cx, cy, r, pincel)
            pincel.color = ContextCompat.getColor(contexto, colorInsignia)
            lienzo.drawCircle(cx, cy, r - 2 * d, pincel)
            val g = r * 0.5f
            ponerGlifo(contexto, R.drawable.ic_mapa_campana, cx - g, cy - g, cx + g, cy + g, lienzo)
        }
        return BitmapDrawable(contexto.resources, bitmap)
    }

    private fun ponerGlifo(contexto: Context, @DrawableRes glifo: Int, izq: Float, arriba: Float, der: Float, abajo: Float, lienzo: Canvas) {
        val dibujo = ContextCompat.getDrawable(contexto, glifo) ?: return
        dibujo.setBounds(izq.toInt(), arriba.toInt(), der.toInt(), abajo.toInt())
        dibujo.draw(lienzo)
    }
}
