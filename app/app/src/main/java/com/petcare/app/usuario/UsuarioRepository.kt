package com.petcare.app.usuario

import com.petcare.app.common.ApiClient
import com.petcare.app.common.Resultado
import com.petcare.app.common.SessionManager
import com.petcare.app.common.llamarApi

/** Intermediario entre las pantallas y la API: las pantallas no hablan con Retrofit directamente. */
class UsuarioRepository(private val api: UsuarioApi = ApiClient.crear(UsuarioApi::class.java)) {

    suspend fun iniciarSesion(correo: String, contrasena: String): Resultado<LoginRespuesta> {
        val resultado = llamarApi { api.iniciarSesion(LoginPeticion(correo.trim(), contrasena)) }
        if (resultado is Resultado.Exito) {
            SessionManager.guardar(resultado.dato.token, resultado.dato.usuario.nombre)
        }
        return resultado
    }

    suspend fun obtenerPerfil(): Resultado<UsuarioRespuesta> = llamarApi { api.obtenerPerfil() }

    /** Guarda el nombre y el correo; si sale bien, actualiza el nombre que recuerda la sesión. */
    suspend fun actualizarPerfil(nombre: String, correo: String): Resultado<UsuarioRespuesta> {
        val resultado = llamarApi { api.actualizarPerfil(PerfilPeticion(nombre.trim(), correo.trim())) }
        if (resultado is Resultado.Exito) SessionManager.guardarNombre(resultado.dato.nombre)
        return resultado
    }

    suspend fun cambiarContrasena(actual: String, nueva: String): Resultado<MensajeRespuesta> =
        llamarApi { api.cambiarContrasena(ContrasenaPeticion(actual, nueva)) }

    /** Registra al usuario. Según el ERS (RF01), después debe iniciar sesión desde el login. */
    suspend fun registrar(nombre: String, correo: String, contrasena: String): Resultado<UsuarioRespuesta> =
        llamarApi { api.registrar(RegistroPeticion(nombre.trim(), correo.trim(), contrasena)) }
}
