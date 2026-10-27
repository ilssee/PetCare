package com.petcare.app.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.petcare.app.R
import com.petcare.app.databinding.VistaEstadoVacioBinding

/** Configura la vista de "todavía no hay nada" que se repite en varias secciones. */
fun VistaEstadoVacioBinding.configurar(
    @DrawableRes icono: Int,
    @StringRes titulo: Int,
    @StringRes mensaje: Int,
    @StringRes textoBoton: Int = R.string.boton_agregar_mascota,
    alTocarBoton: () -> Unit,
) {
    this.icono.setImageResource(icono)
    this.titulo.setText(titulo)
    this.mensaje.setText(mensaje)
    this.boton.setText(textoBoton)
    this.boton.setOnClickListener { alTocarBoton() }
}
