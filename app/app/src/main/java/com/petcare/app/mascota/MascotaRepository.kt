package com.petcare.app.mascota

import com.petcare.app.common.ApiClient
import com.petcare.app.common.Resultado
import com.petcare.app.common.llamarApi

/** Intermediario entre las pantallas de mascotas y la API. */
class MascotaRepository(private val api: MascotaApi = ApiClient.crear(MascotaApi::class.java)) {
    suspend fun listar(): Resultado<List<MascotaRespuesta>> = llamarApi { api.listar() }
    suspend fun obtener(id: Int): Resultado<MascotaRespuesta> = llamarApi { api.obtener(id) }
    suspend fun crear(datos: MascotaPeticion): Resultado<MascotaRespuesta> = llamarApi { api.crear(datos) }
    suspend fun editar(id: Int, datos: MascotaPeticion): Resultado<MascotaRespuesta> = llamarApi { api.editar(id, datos) }
    suspend fun inactivar(id: Int): Resultado<MascotaRespuesta> = llamarApi { api.inactivar(id) }
    suspend fun reactivar(id: Int): Resultado<MascotaRespuesta> = llamarApi { api.reactivar(id) }
    suspend fun eliminar(id: Int): Resultado<Unit> = llamarApi { api.eliminar(id) }
}
