package com.petcare.app.common

import android.content.Context
import com.google.gson.Gson
import com.petcare.app.R
import retrofit2.Response
import java.io.IOException

/** Resultado de una operación: o salió bien, o trae el mensaje de error para mostrar. */
sealed class Resultado<out T> {
    data class Exito<T>(val dato: T) : Resultado<T>()
    data class Fallo(val mensaje: String) : Resultado<Nothing>()
}

/** Formato de los errores del backend: {"error": "mensaje"} */
data class ErrorRespuesta(val error: String?)

const val SIN_CONEXION = "sin_conexion"

/** Ejecuta una llamada a la API y la convierte en Resultado, con el mensaje del backend si falla. */
suspend fun <T> llamarApi(peticion: suspend () -> Response<T>): Resultado<T> {
    return try {
        val respuesta = peticion()
        val cuerpo = respuesta.body()
        if (respuesta.isSuccessful && cuerpo != null) {
            Resultado.Exito(cuerpo)
        } else {
            val texto = respuesta.errorBody()?.string()
            val mensaje = runCatching { Gson().fromJson(texto, ErrorRespuesta::class.java).error }.getOrNull()
            Resultado.Fallo(mensaje ?: "Error del servidor (${respuesta.code()})")
        }
    } catch (e: IOException) {
        Resultado.Fallo(SIN_CONEXION)
    }
}

/**
 * Para las llamadas que responden "listo, sin contenido" (204), como eliminar.
 * Con ese código no llega ningún cuerpo, así que basta con que la respuesta sea exitosa.
 */
suspend fun llamarApiSinContenido(peticion: suspend () -> Response<Unit>): Resultado<Unit> {
    return try {
        val respuesta = peticion()
        if (respuesta.isSuccessful) {
            Resultado.Exito(Unit)
        } else {
            val texto = respuesta.errorBody()?.string()
            val mensaje = runCatching { Gson().fromJson(texto, ErrorRespuesta::class.java).error }.getOrNull()
            Resultado.Fallo(mensaje ?: "Error del servidor (${respuesta.code()})")
        }
    } catch (e: IOException) {
        Resultado.Fallo(SIN_CONEXION)
    }
}

/** Texto listo para mostrar al usuario (traduce el caso "sin conexión"). */
fun Resultado.Fallo.paraMostrar(contexto: Context): String =
    if (mensaje == SIN_CONEXION) contexto.getString(R.string.error_sin_conexion) else mensaje
