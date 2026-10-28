package com.petcare.app.common

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.petcare.app.R
import com.petcare.app.databinding.HojaOpcionesBinding
import com.petcare.app.databinding.ItemOpcionHojaBinding

/** Una fila de la hoja de opciones. [peligro] la pinta de rojo (por ejemplo, eliminar). */
class OpcionHoja(
    @DrawableRes val icono: Int,
    @StringRes val texto: Int,
    val peligro: Boolean = false,
    val alElegir: () -> Unit,
)

/** Muestra una hoja que sube desde abajo con un título y una lista de opciones. */
fun Context.mostrarHojaOpciones(titulo: String, opciones: List<OpcionHoja>) {
    val inflador = LayoutInflater.from(this)
    val hoja = HojaOpcionesBinding.inflate(inflador)
    val dialogo = BottomSheetDialog(this)

    hoja.titulo.text = titulo
    opciones.forEach { opcion ->
        val fila = ItemOpcionHojaBinding.inflate(inflador, hoja.opciones, false)
        val color = ContextCompat.getColor(this, if (opcion.peligro) R.color.petcare_error else R.color.petcare_text)
        fila.icono.setImageResource(opcion.icono)
        fila.icono.imageTintList = ColorStateList.valueOf(color)
        fila.texto.setText(opcion.texto)
        fila.texto.setTextColor(color)
        fila.root.setOnClickListener {
            dialogo.dismiss()
            opcion.alElegir()
        }
        hoja.opciones.addView(fila.root)
    }

    dialogo.setContentView(hoja.root)
    dialogo.show()
    // El fondo propio de la hoja ya trae las esquinas superiores redondeadas.
    dialogo.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        ?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
}
