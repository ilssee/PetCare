package com.petcare.app.mapa

import com.petcare.app.common.ApiClient
import com.petcare.app.common.Resultado
import com.petcare.app.common.llamarApi

/** Intermediario entre la pantalla del mapa y la API. */
class MapaRepository(private val api: MapaApi = ApiClient.crear(MapaApi::class.java)) {

    private fun <T> Set<T>.comoTexto(): String? = if (isEmpty()) null else joinToString(",")

    suspend fun veterinarias(
        filtros: FiltrosVeterinarias,
        orden: String?,
        lat: Double?,
        lng: Double?,
    ): Resultado<List<VeterinariaRespuesta>> = llamarApi {
        api.veterinarias(
            filtros.tipos.comoTexto(),
            filtros.especies.comoTexto(),
            filtros.servicios.comoTexto(),
            if (filtros.disponibleAhora) true else null,
            orden,
            lat,
            lng,
        )
    }

    suspend fun campanas(
        filtros: FiltrosCampanas,
        orden: String?,
        lat: Double?,
        lng: Double?,
    ): Resultado<List<CampanaRespuesta>> = llamarApi {
        api.campanas(
            filtros.estados.comoTexto(),
            filtros.especies.comoTexto(),
            filtros.servicios.comoTexto(),
            filtros.costos.comoTexto(),
            if (filtros.disponibleAhora) true else null,
            orden,
            lat,
            lng,
        )
    }

    suspend fun buscar(texto: String, lat: Double?, lng: Double?): Resultado<List<ResultadoBusquedaRespuesta>> =
        llamarApi { api.buscar(texto, lat, lng) }

    suspend fun filtros(): Resultado<CatalogosRespuesta> = llamarApi { api.filtros() }

    suspend fun detalleDeCampana(id: Int, lat: Double?, lng: Double?): Resultado<DetalleCampanaRespuesta> =
        llamarApi { api.detalleCampana(id, lat, lng) }

    suspend fun perfilDeVeterinaria(id: Int, lat: Double?, lng: Double?): Resultado<PerfilVeterinariaRespuesta> =
        llamarApi { api.perfil(id, lat, lng) }
}
