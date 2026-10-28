package com.petcare.app.cuidado

import com.petcare.app.common.ApiClient
import com.petcare.app.common.Resultado
import com.petcare.app.common.llamarApi

/** Intermediario entre las pantallas de cuidados y la API. */
class CuidadoRepository(private val api: CuidadoApi = ApiClient.crear(CuidadoApi::class.java)) {
    suspend fun recomendaciones(mascotaId: Int): Resultado<List<RecomendacionRespuesta>> =
        llamarApi { api.recomendaciones(mascotaId) }

    suspend fun activarRecomendados(
        mascotaId: Int,
        cuidados: List<CuidadoElegido>,
    ): Resultado<ActivarRecomendadosRespuesta> =
        llamarApi { api.activarRecomendados(mascotaId, ActivarRecomendadosPeticion(cuidados)) }
}
