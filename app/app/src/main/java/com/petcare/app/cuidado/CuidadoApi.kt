package com.petcare.app.cuidado

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/** Rutas de la API de cuidados (ver backend/src/modules/regla y backend/src/modules/cuidado). Exigen sesión. */
interface CuidadoApi {
    @GET("mascotas/{id}/recomendaciones")
    suspend fun recomendaciones(@Path("id") mascotaId: Int): Response<List<RecomendacionRespuesta>>

    @POST("mascotas/{id}/cuidados/recomendados")
    suspend fun activarRecomendados(
        @Path("id") mascotaId: Int,
        @Body datos: ActivarRecomendadosPeticion,
    ): Response<ActivarRecomendadosRespuesta>
}
