package com.petcare.app.notificacion

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.petcare.app.MainActivity
import com.petcare.app.R
import com.petcare.app.common.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Recibe lo que envía Firebase: el token del celular y los avisos con la app abierta. */
class PetCareMensajes : FirebaseMessagingService() {
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Firebase renueva el token de vez en cuando: hay que avisar al servidor.
    override fun onNewToken(token: String) {
        alcance.launch { Notificaciones.registrarDispositivo() }
    }

    // Con la app cerrada o en segundo plano, Android muestra el aviso por su cuenta.
    // Con la app abierta, hay que mostrarlo a mano.
    override fun onMessageReceived(mensaje: RemoteMessage) {
        val aviso = mensaje.notification ?: return
        if (!SessionManager.haySesion || !Notificaciones.tienePermiso(this)) return

        val abrirCuidados = Intent(this, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_SECCION, R.id.nav_cuidados)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendiente = PendingIntent.getActivity(
            this, 0, abrirCuidados, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notificacion = NotificationCompat.Builder(this, Notificaciones.CANAL_RECORDATORIOS)
            .setSmallIcon(R.drawable.ic_huella)
            .setContentTitle(aviso.title)
            .setContentText(aviso.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(aviso.body))
            .setColor(getColor(R.color.petcare_primary))
            .setAutoCancel(true)
            .setContentIntent(pendiente)
            .build()
        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notificacion)
    }
}
