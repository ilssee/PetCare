package com.petcare.app.notificacion

import android.Manifest
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.petcare.app.databinding.VistaAvisoNotificacionesBinding
import kotlinx.coroutines.launch

/**
 * Franja que se muestra en Cronograma y Cuidados mientras las notificaciones de PetCare están apagadas.
 * Aparece sola y desaparece cuando se activan. El botón Activar muestra el cuadro de Android; si Android ya
 * no lo muestra (el usuario lo rechazó varias veces), abre los ajustes de notificaciones de PetCare.
 */
class AvisoNotificaciones(private val fragmento: Fragment) {

    private var vista: VistaAvisoNotificacionesBinding? = null
    private var momentoDelPedido = 0L

    private val pedirPermiso = fragmento.registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) {
            // Con el permiso, este celular ya puede recibir los avisos de la sesión.
            fragmento.lifecycleScope.launch { Notificaciones.registrarDispositivo() }
        } else if (SystemClock.elapsedRealtime() - momentoDelPedido < RESPUESTA_SIN_CUADRO_MS) {
            // Respondió casi al instante: Android no mostró ningún cuadro, así que solo queda ir a los ajustes.
            abrirAjustes()
        }
        actualizar()
    }

    /** Enlaza la franja de la pantalla. Llamar al crear la vista. */
    fun vincular(franja: VistaAvisoNotificacionesBinding) {
        vista = franja
        franja.botonActivarNotificaciones.setOnClickListener {
            momentoDelPedido = SystemClock.elapsedRealtime()
            pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        actualizar()
    }

    /** Suelta la franja. Llamar al destruir la vista. */
    fun desvincular() {
        vista = null
    }

    /** Muestra u oculta la franja según el permiso. Llamar también al volver a la pantalla (por ejemplo, de los ajustes). */
    fun actualizar() {
        val franja = vista ?: return
        val contexto = fragmento.context ?: return
        franja.root.visibility = if (Notificaciones.tienePermiso(contexto)) View.GONE else View.VISIBLE
    }

    private fun abrirAjustes() {
        val contexto = fragmento.context ?: return
        contexto.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName),
        )
    }

    private companion object {
        const val RESPUESTA_SIN_CUADRO_MS = 300L
    }
}
