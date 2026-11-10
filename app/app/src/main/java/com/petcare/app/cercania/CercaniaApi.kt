package com.petcare.app.cercania

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

/** Rutas de la API de cercanía (ver backend/src/modules/cercania). Exigen sesión. */
interface CercaniaApi {
    @GET("cercania")
    suspend fun obtener(): Response<ConfiguracionCercania>

    @PUT("cercania")
    suspend fun guardar(@Body datos: ConfiguracionCercania): Response<ConfiguracionCercania>
}
