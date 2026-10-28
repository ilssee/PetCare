package com.petcare.app.cuidado

import com.petcare.app.common.ApiClient
import com.petcare.app.common.Resultado
import com.petcare.app.common.llamarApi
import com.petcare.app.common.llamarApiSinContenido

/** Intermediario entre las pantallas de cuidados y la API. */
class CuidadoRepository(private val api: CuidadoApi = ApiClient.crear(CuidadoApi::class.java)) {
    suspend fun recomendaciones(mascotaId: Int): Resultado<List<RecomendacionRespuesta>> =
        llamarApi { api.recomendaciones(mascotaId) }

    suspend fun informacionPreventiva(mascotaId: Int): Resultado<List<RecomendacionRespuesta>> =
        llamarApi { api.informacionPreventiva(mascotaId) }

    suspend fun activarRecomendados(
        mascotaId: Int,
        cuidados: List<CuidadoElegido>,
    ): Resultado<ActivarRecomendadosRespuesta> =
        llamarApi { api.activarRecomendados(mascotaId, ActivarRecomendadosPeticion(cuidados)) }

    suspend fun listar(mascotaId: Int, estado: String): Resultado<ListaCuidadosRespuesta> =
        llamarApi { api.listar(mascotaId, estado) }

    suspend fun crearPersonalizado(mascotaId: Int, datos: CuidadoPeticion): Resultado<GuardarCuidadoRespuesta> =
        llamarApi { api.crearPersonalizado(mascotaId, datos) }

    /** Los cuidados pendientes de todas las mascotas activas, o solo de [mascotaId]. */
    suspend fun cronograma(mascotaId: Int?): Resultado<CronogramaRespuesta> =
        llamarApi { api.cronograma(mascotaId, "pendientes") }

    suspend fun conteo(): Resultado<ConteoCuidados> = llamarApi { api.conteo() }

    suspend fun obtener(cuidadoId: Int): Resultado<CuidadoRespuesta> = llamarApi { api.obtener(cuidadoId) }

    suspend fun editar(cuidadoId: Int, datos: CuidadoPeticion): Resultado<GuardarCuidadoRespuesta> =
        llamarApi { api.editar(cuidadoId, datos) }

    suspend fun eliminar(cuidadoId: Int): Resultado<Unit> = llamarApiSinContenido { api.eliminar(cuidadoId) }

    /** RF16 y RF17: registra la actividad. Devuelve el cuidado ya actualizado, con su nueva fecha. */
    suspend fun completar(cuidadoId: Int, datos: CompletarPeticion): Resultado<GuardarCuidadoRespuesta> =
        llamarApi { api.completar(cuidadoId, datos) }

    /** RF18: agrega o modifica la nota de una actividad. */
    suspend fun guardarNota(actividadId: Int, nota: String): Resultado<GuardarCuidadoRespuesta> =
        llamarApi { api.guardarNota(actividadId, NotaPeticion(nota)) }

    suspend fun eliminarNota(actividadId: Int): Resultado<GuardarCuidadoRespuesta> =
        llamarApi { api.eliminarNota(actividadId) }

    /** RF19: anula una actividad completada por error. */
    suspend fun anular(actividadId: Int): Resultado<GuardarCuidadoRespuesta> = llamarApi { api.anular(actividadId) }
}
