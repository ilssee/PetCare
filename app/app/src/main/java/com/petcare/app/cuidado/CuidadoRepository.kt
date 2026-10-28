package com.petcare.app.cuidado

import com.petcare.app.common.ApiClient
import com.petcare.app.common.Resultado
import com.petcare.app.common.llamarApi
import com.petcare.app.common.llamarApiSinContenido

/** Intermediario entre las pantallas de cuidados y la API. */
class CuidadoRepository(private val api: CuidadoApi = ApiClient.crear(CuidadoApi::class.java)) {
    suspend fun recomendaciones(mascotaId: Int): Resultado<List<RecomendacionRespuesta>> =
        llamarApi { api.recomendaciones(mascotaId) }

    suspend fun activarRecomendados(
        mascotaId: Int,
        cuidados: List<CuidadoElegido>,
    ): Resultado<ActivarRecomendadosRespuesta> =
        llamarApi { api.activarRecomendados(mascotaId, ActivarRecomendadosPeticion(cuidados)) }

    suspend fun listar(mascotaId: Int, estado: String): Resultado<ListaCuidadosRespuesta> =
        llamarApi { api.listar(mascotaId, estado) }

    suspend fun crearPersonalizado(mascotaId: Int, datos: CuidadoPeticion): Resultado<GuardarCuidadoRespuesta> =
        llamarApi { api.crearPersonalizado(mascotaId, datos) }

    suspend fun obtener(cuidadoId: Int): Resultado<CuidadoRespuesta> = llamarApi { api.obtener(cuidadoId) }

    suspend fun editar(cuidadoId: Int, datos: CuidadoPeticion): Resultado<GuardarCuidadoRespuesta> =
        llamarApi { api.editar(cuidadoId, datos) }

    suspend fun eliminar(cuidadoId: Int): Resultado<Unit> = llamarApiSinContenido { api.eliminar(cuidadoId) }
}
