package com.petcare.app.common

import com.petcare.app.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Cliente HTTP compartido. La dirección de la API se define en build.gradle.kts (API_BASE_URL).
 * Si hay una sesión iniciada, agrega el token a cada petición. Cada módulo pide aquí su interfaz con crear(...).
 */
object ApiClient {
    /**
     * Se ejecuta cuando el servidor rechaza la sesión (por ejemplo, porque el usuario cambió su contraseña
     * desde otro dispositivo). La app la usa para volver al inicio de sesión.
     */
    var alInvalidarseSesion: (() -> Unit)? = null

    private val cliente: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { cadena ->
                val token = SessionManager.token
                val peticion = if (token.isNullOrEmpty()) {
                    cadena.request()
                } else {
                    cadena.request().newBuilder().header("Authorization", "Bearer $token").build()
                }
                val respuesta = cadena.proceed(peticion)
                // 401 con una sesión iniciada: la sesión ya no es válida. Se cierra una sola vez.
                if (respuesta.code == 401 && !token.isNullOrEmpty() && SessionManager.token == token) {
                    SessionManager.cerrar()
                    alInvalidarseSesion?.invoke()
                }
                respuesta
            }
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun <T> crear(servicio: Class<T>): T = retrofit.create(servicio)
}
