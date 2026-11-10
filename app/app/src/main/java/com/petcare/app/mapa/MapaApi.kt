package com.petcare.app.mapa

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Rutas de la API del mapa (ver backend/src/modules/mapa). Exigen sesión.
 * Las listas se envían como texto separado por comas ("perro,gato").
 */
interface MapaApi {
    @GET("mapa/veterinarias")
    suspend fun veterinarias(
        @Query("tipo") tipo: String?,
        @Query("especie") especie: String?,
        @Query("servicio") servicio: String?,
        @Query("disponibleAhora") disponibleAhora: Boolean?,
        @Query("ordenar") ordenar: String?,
        @Query("lat") lat: Double?,
        @Query("lng") lng: Double?,
    ): Response<List<VeterinariaRespuesta>>

    @GET("mapa/veterinarias/{id}")
    suspend fun perfil(
        @Path("id") id: Int,
        @Query("lat") lat: Double?,
        @Query("lng") lng: Double?,
    ): Response<PerfilVeterinariaRespuesta>

    @GET("mapa/campanas")
    suspend fun campanas(
        @Query("estado") estado: String?,
        @Query("especie") especie: String?,
        @Query("servicio") servicio: String?,
        @Query("costo") costo: String?,
        @Query("disponibleAhora") disponibleAhora: Boolean?,
        @Query("ordenar") ordenar: String?,
        @Query("lat") lat: Double?,
        @Query("lng") lng: Double?,
    ): Response<List<CampanaRespuesta>>

    @GET("mapa/buscar")
    suspend fun buscar(
        @Query("texto") texto: String,
        @Query("lat") lat: Double?,
        @Query("lng") lng: Double?,
    ): Response<List<ResultadoBusquedaRespuesta>>

    @GET("mapa/filtros")
    suspend fun filtros(): Response<CatalogosRespuesta>
}
