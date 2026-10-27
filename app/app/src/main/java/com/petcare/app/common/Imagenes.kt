package com.petcare.app.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** Ayudas para preparar las fotos antes de enviarlas al servidor. */
object Imagenes {
    private const val LADO_MAXIMO = 1024
    private const val CALIDAD_JPEG = 85

    /**
     * Lee la imagen elegida de la galería, la reduce (máximo 1024 px por lado) y la convierte a JPEG.
     * Así la subida es rápida y pesa mucho menos de los 5 MB que admite el servidor.
     * Devuelve null si la imagen no se puede leer.
     */
    suspend fun prepararFoto(contexto: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val origen = ImageDecoder.createSource(contexto.contentResolver, uri)
            val bitmap = ImageDecoder.decodeBitmap(origen) { decodificador, info, _ ->
                decodificador.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val mayor = maxOf(info.size.width, info.size.height)
                if (mayor > LADO_MAXIMO) {
                    val escala = LADO_MAXIMO.toFloat() / mayor
                    decodificador.setTargetSize(
                        (info.size.width * escala).toInt().coerceAtLeast(1),
                        (info.size.height * escala).toInt().coerceAtLeast(1),
                    )
                }
            }
            ByteArrayOutputStream().use { salida ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, salida)
                salida.toByteArray()
            }
        }.getOrNull()
    }
}
