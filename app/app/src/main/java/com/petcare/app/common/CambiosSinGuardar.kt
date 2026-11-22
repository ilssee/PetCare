package com.petcare.app.common

import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.petcare.app.R

/**
 * Cierra la pantalla. Si [hayCambios] indica que el usuario escribió o cambió algo sin guardar,
 * antes pregunta si quiere descartarlo. Es el mismo aviso en todas las pantallas con formulario.
 */
fun AppCompatActivity.salirConfirmando(hayCambios: () -> Boolean) {
    if (!hayCambios()) {
        finish()
        return
    }
    MaterialAlertDialogBuilder(this)
        .setTitle(R.string.descartar_cambios_titulo)
        .setMessage(R.string.descartar_cambios_mensaje)
        .setNegativeButton(R.string.descartar_cambios_seguir, null)
        .setPositiveButton(R.string.descartar_cambios_descartar) { _, _ -> finish() }
        .show()
}

/** Hace que el botón "atrás" del teléfono y el gesto de retroceso pasen por el mismo aviso. */
fun AppCompatActivity.confirmarAlRetroceder(hayCambios: () -> Boolean) {
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() = salirConfirmando(hayCambios)
    })
}
