package com.petcare.app.notificacion

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.petcare.app.MainActivity
import com.petcare.app.R
import com.petcare.app.common.SessionManager
import com.petcare.app.cuidado.InfoPreventivaActivity
import com.petcare.app.mapa.CampanaDetalleActivity
import com.petcare.app.mapa.MarcadoresMapa
import com.petcare.app.mapa.PerfilVeterinariaActivity
import com.petcare.app.mascota.Especies
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

    // El servidor envía solo datos, así que la app arma siempre el aviso (con la app abierta, en segundo
    // plano o cerrada). El aviso lleva el ícono de la app y el texto que envía el servidor.
    override fun onMessageReceived(mensaje: RemoteMessage) {
        val titulo = mensaje.data["titulo"] ?: mensaje.notification?.title ?: return
        val cuerpo = mensaje.data["cuerpo"] ?: mensaje.notification?.body ?: return
        if (!SessionManager.haySesion || !Notificaciones.tienePermiso(this)) return

        // Al tocar el aviso se abre su pantalla (perfil, campaña o información preventiva) encima de su sección:
        // los avisos de lugares cercanos quedan sobre el mapa y los demás sobre los cuidados, para volver con "atrás".
        val notificacionId = System.currentTimeMillis().toInt()
        val seccion = if (mensaje.data["seccion"] == "mapa") R.id.nav_mapa else R.id.nav_cuidados
        val abrirSeccion = Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_SECCION, seccion)
        val pila = TaskStackBuilder.create(this).addNextIntent(abrirSeccion)
        destinoDelAviso(mensaje.data)?.let { pila.addNextIntent(it) }
        val pendiente = pila.getPendingIntent(
            notificacionId, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // El ícono de la mascota (o de la campaña o veterinaria) va como imagen grande, a la derecha del texto.
        val notificacion = NotificationCompat.Builder(this, Notificaciones.CANAL_RECORDATORIOS)
            .setSmallIcon(R.drawable.ic_huella)
            .setLargeIcon(iconoDeAviso(mensaje.data["tipo"], mensaje.data["especie"], mensaje.data["variante"]))
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setColor(getColor(R.color.petcare_primary))
            .setAutoCancel(true)
            .setContentIntent(pendiente)
            .build()
        NotificationManagerCompat.from(this).notify(notificacionId, notificacion)
    }

    /**
     * Pantalla que abre el aviso al tocarlo. Si faltan los datos, se devuelve null y solo se abre la sección.
     *  - campaña de una veterinaria: el perfil de la veterinaria con esa campaña desplegada;
     *  - campaña de una entidad externa: el detalle de la campaña;
     *  - veterinaria nueva: su perfil;
     *  - recomendaciones nuevas: la información preventiva de la mascota con esas recomendaciones desplegadas.
     */
    private fun destinoDelAviso(datos: Map<String, String>): Intent? {
        val lugarId = datos["lugarId"]?.toIntOrNull() ?: 0
        val veterinariaId = datos["veterinariaId"]?.toIntOrNull() ?: 0
        return when (datos["tipo"]) {
            "campana_cercana" -> when {
                lugarId > 0 && veterinariaId > 0 ->
                    PerfilVeterinariaActivity.intent(this, veterinariaId, null, null, campanaId = lugarId)
                lugarId > 0 -> CampanaDetalleActivity.intent(this, lugarId, null, null)
                else -> null
            }
            "veterinaria_cercana" ->
                if (veterinariaId > 0) PerfilVeterinariaActivity.intent(this, veterinariaId, null, null) else null
            "recomendacion" -> {
                val mascotaId = datos["mascotaId"]?.toIntOrNull() ?: 0
                val reglas = datos["reglas"].orEmpty().split(',').mapNotNull { it.trim().toIntOrNull() }
                if (mascotaId > 0) InfoPreventivaActivity.intent(this, mascotaId, reglas) else null
            }
            else -> null
        }
    }

    /**
     * Ícono redondo del aviso, a la derecha del texto:
     *  - cuidados y recomendaciones: el dibujo de la especie de la mascota, sobre su color (no su foto);
     *  - campaña nueva: el mismo dibujo y color que su marcador del mapa (verde si está activa, naranja si es próxima);
     *  - veterinaria nueva: el mismo dibujo y color que su marcador del mapa (azul si es privada, rosado si es municipal).
     * [variante] la envía el servidor: "activa" o "proxima" (campañas), "privada" o "municipal" (veterinarias).
     */
    private fun iconoDeAviso(tipo: String?, especie: String?, variante: String?): Bitmap {
        val lado = (64 * resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888)
        val lienzo = Canvas(bitmap)

        val fondo: Int
        val dibujo: Int
        val tinteBlanco: Boolean
        val margen: Float
        when {
            tipo == "campana_cercana" -> {
                // Los dibujos del mapa ya son blancos: no se tiñen.
                fondo = MarcadoresMapa.colorDeCampana(variante ?: "activa"); dibujo = R.drawable.ic_mapa_campana
                tinteBlanco = false; margen = 0.2f
            }
            tipo == "veterinaria_cercana" -> {
                fondo = MarcadoresMapa.colorDeVeterinaria(variante ?: "privada"); dibujo = R.drawable.ic_mapa_veterinaria
                tinteBlanco = false; margen = 0.2f
            }
            !especie.isNullOrEmpty() -> {
                fondo = Especies.color(especie); dibujo = Especies.ilustracion(especie); tinteBlanco = false; margen = 0.12f
            }
            else -> {
                fondo = R.color.petcare_primary; dibujo = R.drawable.ic_huella; tinteBlanco = true; margen = 0.24f
            }
        }

        val pintura = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(this@PetCareMensajes, fondo) }
        lienzo.drawCircle(lado / 2f, lado / 2f, lado / 2f, pintura)

        val icono = ContextCompat.getDrawable(this, dibujo)?.mutate() ?: return bitmap
        if (tinteBlanco) DrawableCompat.setTint(icono, Color.WHITE)
        val m = (lado * margen).toInt()
        icono.setBounds(m, m, lado - m, lado - m)
        icono.draw(lienzo)
        return bitmap
    }
}
