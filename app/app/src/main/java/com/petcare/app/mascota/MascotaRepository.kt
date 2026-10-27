package com.petcare.app.mascota

import com.petcare.app.common.ApiClient
import com.petcare.app.common.Resultado
import com.petcare.app.common.llamarApi
import com.petcare.app.common.llamarApiSinContenido
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/** Intermediario entre las pantallas de mascotas y la API. */
class MascotaRepository(private val api: MascotaApi = ApiClient.crear(MascotaApi::class.java)) {
    suspend fun listar(): Resultado<List<MascotaRespuesta>> = llamarApi { api.listar() }
    suspend fun obtener(id: Int): Resultado<MascotaRespuesta> = llamarApi { api.obtener(id) }
    suspend fun crear(datos: MascotaPeticion): Resultado<MascotaRespuesta> = llamarApi { api.crear(datos) }
    suspend fun editar(id: Int, datos: MascotaPeticion): Resultado<MascotaRespuesta> = llamarApi { api.editar(id, datos) }
    suspend fun inactivar(id: Int): Resultado<MascotaRespuesta> = llamarApi { api.inactivar(id) }
    suspend fun reactivar(id: Int): Resultado<MascotaRespuesta> = llamarApi { api.reactivar(id) }
    /** Envía la foto (ya reducida y en JPEG) como archivo adjunto en el campo "foto". */
    suspend fun subirFoto(id: Int, jpeg: ByteArray): Resultado<MascotaRespuesta> {
        val parte = MultipartBody.Part.createFormData("foto", "foto.jpg", jpeg.toRequestBody("image/jpeg".toMediaType()))
        return llamarApi { api.subirFoto(id, parte) }
    }

    suspend fun quitarFoto(id: Int): Resultado<MascotaRespuesta> = llamarApi { api.quitarFoto(id) }
    suspend fun eliminar(id: Int): Resultado<Unit> = llamarApiSinContenido { api.eliminar(id) }
}
