package com.petcare.app.usuario

import android.text.InputType
import android.widget.EditText
import android.widget.ImageView
import com.petcare.app.R

/**
 * Alterna entre mostrar y ocultar el texto de un campo de contraseña.
 * El icono acompaña al estado: ojo normal con la contraseña oculta, ojo tachado cuando se está viendo.
 */
fun EditText.alternarVisibilidadContrasena(icono: ImageView) {
    val variacionActual = inputType and InputType.TYPE_MASK_VARIATION
    val estabaVisible = variacionActual == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

    // Cambiar el tipo de entrada reinicia la tipografía, por eso se guarda y se restaura.
    val tipografia = typeface
    inputType = InputType.TYPE_CLASS_TEXT or
        if (estabaVisible) InputType.TYPE_TEXT_VARIATION_PASSWORD else InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
    typeface = tipografia
    setSelection(text.length)

    icono.setImageResource(if (estabaVisible) R.drawable.ic_ojo else R.drawable.ic_ojo_tachado)
}
