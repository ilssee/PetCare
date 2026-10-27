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
    private val cliente: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { cadena ->
                val token = SessionManager.token
                val peticion = if (token.isNullOrEmpty()) {
                    cadena.request()
                } else {
                    cadena.request().newBuilder().header("Authorization", "Bearer $token").build()
                }
                cadena.proceed(peticion)
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
