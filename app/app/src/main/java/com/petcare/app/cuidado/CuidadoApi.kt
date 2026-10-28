package com.petcare.app.cuidado

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Rutas de la API de cuidados (ver backend/src/modules/regla y backend/src/modules/cuidado). Exigen sesión. */
interface CuidadoApi {
    @GET("mascotas/{id}/recomendaciones")
    suspend fun recomendaciones(@Path("id") mascotaId: Int): Response<List<RecomendacionRespuesta>>

    @POST("mascotas/{id}/cuidados/recomendados")
    suspend fun activarRecomendados(
        @Path("id") mascotaId: Int,
        @Body datos: ActivarRecomendadosPeticion,
    ): Response<ActivarRecomendadosRespuesta>

    /** estado: todos, pendientes o finalizados. */
    @GET("mascotas/{id}/cuidados")
    suspend fun listar(
        @Path("id") mascotaId: Int,
        @Query("estado") estado: String,
    ): Response<ListaCuidadosRespuesta>

    @POST("mascotas/{id}/cuidados")
    suspend fun crearPersonalizado(
        @Path("id") mascotaId: Int,
        @Body datos: CuidadoPeticion,
    ): Response<GuardarCuidadoRespuesta>

    /** Cronograma: cuidados de todas las mascotas activas (o de una sola). estado: pendientes, finalizados o todos. */
    @GET("cuidados")
    suspend fun cronograma(
        @Query("mascotaId") mascotaId: Int?,
        @Query("estado") estado: String,
    ): Response<CronogramaRespuesta>

    @GET("cuidados/{id}")
    suspend fun obtener(@Path("id") cuidadoId: Int): Response<CuidadoRespuesta>

    @PUT("cuidados/{id}")
    suspend fun editar(
        @Path("id") cuidadoId: Int,
        @Body datos: CuidadoPeticion,
    ): Response<GuardarCuidadoRespuesta>

    @DELETE("cuidados/{id}")
    suspend fun eliminar(@Path("id") cuidadoId: Int): Response<Unit>
}
