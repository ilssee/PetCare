package com.petcare.app.notificacion

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.petcare.app.R
import com.petcare.app.common.ApiClient
import com.petcare.app.common.SessionManager
import com.petcare.app.common.llamarApiSinContenido
import kotlinx.coroutines.tasks.await

/** Notificaciones push (Firebase): canal, permiso y registro del celular en el servidor. */
object Notificaciones {
    /** Debe coincidir con CANAL_RECORDATORIOS del backend. */
    const val CANAL_RECORDATORIOS = "recordatorios"

    private val api: DispositivoApi by lazy { ApiClient.crear(DispositivoApi::class.java) }

    fun crearCanal(contexto: Context) {
        val canal = NotificationChannel(
            CANAL_RECORDATORIOS,
            contexto.getString(R.string.notificacion_canal),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = contexto.getString(R.string.notificacion_canal_descripcion) }
        contexto.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    fun tienePermiso(contexto: Context): Boolean =
        ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** Guarda en el servidor el token de este celular para la sesión actual. Si falla, se reintenta al abrir la app. */
    suspend fun registrarDispositivo() {
        if (!SessionManager.haySesion) return
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull() ?: return
        llamarApiSinContenido { api.registrar(TokenPeticion(token)) }
    }

    /** Al cerrar sesión: este celular deja de recibir los avisos de esa cuenta. Debe llamarse antes de borrar la sesión. */
    suspend fun darDeBaja() {
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull() ?: return
        llamarApiSinContenido { api.darDeBaja(TokenPeticion(token)) }
    }
}
