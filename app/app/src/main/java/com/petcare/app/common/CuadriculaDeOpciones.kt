package com.petcare.app.common

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout

/**
 * Acomoda vistas en una cuadrícula ordenada: [columnas] por fila, todas del mismo ancho y con la misma altura
 * dentro de cada fila. Si en la última fila sobran columnas, sus vistas se reparten todo el ancho
 * (una sola ocupa la fila completa).
 */
object CuadriculaDeOpciones {

    fun crear(contexto: Context, vistas: List<View>, columnas: Int, separacion: Int): LinearLayout {
        val cuadricula = LinearLayout(contexto).apply { orientation = LinearLayout.VERTICAL }
        vistas.chunked(columnas.coerceAtLeast(1)).forEachIndexed { numeroFila, grupo ->
            val fila = LinearLayout(contexto).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { topMargin = if (numeroFila == 0) 0 else separacion }
            }
            grupo.forEachIndexed { posicion, vista ->
                (vista.parent as? ViewGroup)?.removeView(vista)
                vista.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                    .apply { marginStart = if (posicion == 0) 0 else separacion }
                fila.addView(vista)
            }
            cuadricula.addView(fila)
        }
        return cuadricula
    }
}
