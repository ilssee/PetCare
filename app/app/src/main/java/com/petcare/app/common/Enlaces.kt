package com.petcare.app.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.petcare.app.R
import java.util.Locale

/**
 * Abre desde la app otras aplicaciones del celular: Google Maps para llegar a un lugar, el marcador para llamar
 * y el navegador o la aplicación de una red social. No necesita claves de Google ni permisos especiales.
 */
object Enlaces {

    /** "Cómo llegar": abre la ruta hacia las coordenadas en Google Maps (o en el navegador si no está instalado). */
    fun comoLlegar(contexto: Context, lat: Double, lng: Double) {
        val destino = String.format(Locale.US, "%.6f,%.6f", lat, lng)
        abrir(contexto, Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$destino")))
    }

    /** Abre el marcador con el número ya escrito; el usuario decide si llama. */
    fun llamar(contexto: Context, telefono: String) {
        abrir(contexto, Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(telefono.trim()))))
    }

    /** Abre un enlace (red social o web). Si no trae "https://", se lo agrega. */
    fun abrirEnlace(contexto: Context, enlace: String) {
        val limpio = enlace.trim()
        val completo = if (limpio.startsWith("http://") || limpio.startsWith("https://")) limpio else "https://$limpio"
        abrir(contexto, Intent(Intent.ACTION_VIEW, Uri.parse(completo)))
    }

    private fun abrir(contexto: Context, intent: Intent) {
        try {
            contexto.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(contexto, R.string.enlace_sin_aplicacion, Toast.LENGTH_SHORT).show()
        }
    }
}
