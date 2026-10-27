package com.petcare.app.usuario

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

/** Rutas de la API del módulo usuario (ver backend/src/modules/usuario). */
interface UsuarioApi {
    @POST("usuarios/registro")
    suspend fun registrar(@Body datos: RegistroPeticion): Response<UsuarioRespuesta>

    @GET("usuarios/yo")
    suspend fun obtenerPerfil(): Response<UsuarioRespuesta>

    @PUT("usuarios/yo")
    suspend fun actualizarPerfil(@Body datos: PerfilPeticion): Response<UsuarioRespuesta>

    @PUT("usuarios/yo/contrasena")
    suspend fun cambiarContrasena(@Body datos: ContrasenaPeticion): Response<MensajeRespuesta>

    @POST("usuarios/login")
    suspend fun iniciarSesion(@Body datos: LoginPeticion): Response<LoginRespuesta>
}
