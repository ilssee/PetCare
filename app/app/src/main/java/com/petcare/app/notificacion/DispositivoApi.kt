package com.petcare.app.notificacion

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class TokenPeticion(val token: String)

/** Rutas de la API para registrar el celular que recibe las notificaciones (ver backend/src/modules/dispositivo). */
interface DispositivoApi {
    @POST("dispositivos")
    suspend fun registrar(@Body datos: TokenPeticion): Response<Unit>

    @POST("dispositivos/baja")
    suspend fun darDeBaja(@Body datos: TokenPeticion): Response<Unit>
}
