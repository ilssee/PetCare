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
