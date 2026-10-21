package com.petcare.app.usuario

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/** Rutas de la API del módulo usuario (ver backend/src/modules/usuario). */
interface UsuarioApi {
    @POST("usuarios/registro")
    suspend fun registrar(@Body datos: RegistroPeticion): Response<UsuarioRespuesta>

    @POST("usuarios/login")
    suspend fun iniciarSesion(@Body datos: LoginPeticion): Response<LoginRespuesta>
}
