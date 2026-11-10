package com.petcare.app.resena

import com.petcare.app.mapa.ResenaRespuesta
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Lo que se envía al escribir o editar una reseña. */
data class ResenaPeticion(val calificacion: Int, val comentario: String?)

/** Todas las reseñas de un organizador, con su resumen. */
data class ListadoResenasRespuesta(val promedio: Double?, val total: Int, val resenas: List<ResenaRespuesta>)

/** Rutas de la API de reseñas (ver backend/src/modules/resena). Exigen sesión. */
interface ResenaApi {
    @GET("resenas/organizadores/{id}")
    suspend fun listar(@Path("id") organizadorId: Int, @Query("orden") orden: String): Response<ListadoResenasRespuesta>

    @POST("resenas/organizadores/{id}")
    suspend fun crear(@Path("id") organizadorId: Int, @Body datos: ResenaPeticion): Response<ResenaRespuesta>

    @PUT("resenas/{id}")
    suspend fun editar(@Path("id") id: Int, @Body datos: ResenaPeticion): Response<ResenaRespuesta>

    @DELETE("resenas/{id}")
    suspend fun eliminar(@Path("id") id: Int): Response<Unit>
}
