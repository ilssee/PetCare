package com.petcare.app.usuario

import android.text.InputType
import android.widget.EditText

/** Alterna entre mostrar y ocultar el texto de un campo de contraseña. */
fun EditText.alternarVisibilidadContrasena() {
    val variacionActual = inputType and InputType.TYPE_MASK_VARIATION
    val estabaVisible = variacionActual == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

    // Cambiar el tipo de entrada reinicia la tipografía, por eso se guarda y se restaura.
    val tipografia = typeface
    inputType = InputType.TYPE_CLASS_TEXT or
        if (estabaVisible) InputType.TYPE_TEXT_VARIATION_PASSWORD else InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
    typeface = tipografia
    setSelection(text.length)
}
