package com.petcare.app.common

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/** Deja espacio arriba para que el contenido no quede debajo de la barra de estado del celular. */
fun View.respetarBarraDeEstado() {
    val relleno = paddingTop
    ViewCompat.setOnApplyWindowInsetsListener(this) { vista, insets ->
        val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        vista.updatePadding(top = relleno + barras.top)
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

/**
 * Deja espacio para la barra de estado, la barra de navegación y el teclado.
 * Con la pantalla de borde a borde el sistema ya no encoge la ventana al abrir el teclado,
 * así que se sube el contenido por encima de él para que lo escrito no quede tapado.
 */
fun View.respetarBarrasYTeclado() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { vista, insets ->
        val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val teclado = insets.getInsets(WindowInsetsCompat.Type.ime())
        vista.setPadding(0, barras.top, 0, maxOf(barras.bottom, teclado.bottom))
        insets
    }
}
