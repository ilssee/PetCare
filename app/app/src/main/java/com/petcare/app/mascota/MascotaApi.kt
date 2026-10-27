package com.petcare.app.mascota

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.Path

/** Rutas de la API del módulo mascota (ver backend/src/modules/mascota). Exigen sesión iniciada. */
interface MascotaApi {
    @GET("mascotas")
    suspend fun listar(): Response<List<MascotaRespuesta>>

    @GET("mascotas/{id}")
    suspend fun obtener(@Path("id") id: Int): Response<MascotaRespuesta>

    @POST("mascotas")
    suspend fun crear(@Body datos: MascotaPeticion): Response<MascotaRespuesta>

    @PUT("mascotas/{id}")
    suspend fun editar(@Path("id") id: Int, @Body datos: MascotaPeticion): Response<MascotaRespuesta>

    @POST("mascotas/{id}/inactivar")
    suspend fun inactivar(@Path("id") id: Int): Response<MascotaRespuesta>

    @POST("mascotas/{id}/reactivar")
    suspend fun reactivar(@Path("id") id: Int): Response<MascotaRespuesta>

    @Multipart
    @POST("mascotas/{id}/foto")
    suspend fun subirFoto(@Path("id") id: Int, @Part foto: MultipartBody.Part): Response<MascotaRespuesta>

    @DELETE("mascotas/{id}/foto")
    suspend fun quitarFoto(@Path("id") id: Int): Response<MascotaRespuesta>

    @DELETE("mascotas/{id}")
    suspend fun eliminar(@Path("id") id: Int): Response<Unit>
}
