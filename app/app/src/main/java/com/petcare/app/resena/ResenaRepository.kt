package com.petcare.app.resena

import com.petcare.app.common.ApiClient
import com.petcare.app.common.Resultado
import com.petcare.app.common.llamarApi
import com.petcare.app.common.llamarApiSinContenido
import com.petcare.app.mapa.ResenaRespuesta

/** Intermediario entre las pantallas de reseñas y la API. */
class ResenaRepository(private val api: ResenaApi = ApiClient.crear(ResenaApi::class.java)) {

    /** [orden] es "recientes" o "calificadas". */
    suspend fun listar(organizadorId: Int, orden: String): Resultado<ListadoResenasRespuesta> =
        llamarApi { api.listar(organizadorId, orden) }

    suspend fun crear(organizadorId: Int, calificacion: Int, comentario: String?): Resultado<ResenaRespuesta> =
        llamarApi { api.crear(organizadorId, ResenaPeticion(calificacion, comentario)) }.avisandoSiSeGuardo()

    suspend fun editar(id: Int, calificacion: Int, comentario: String?): Resultado<ResenaRespuesta> =
        llamarApi { api.editar(id, ResenaPeticion(calificacion, comentario)) }.avisandoSiSeGuardo()

    suspend fun eliminar(id: Int): Resultado<Unit> = llamarApiSinContenido { api.eliminar(id) }.avisandoSiSeGuardo()

    /** Si el cambio se guardó, el mapa debe actualizar las calificaciones cuando se vuelva a él. */
    private fun <T> Resultado<T>.avisandoSiSeGuardo(): Resultado<T> {
        if (this is Resultado.Exito) ResenasCambiaron.marcar()
        return this
    }
}
