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

    /** Registra al usuario. Según el ERS (RF01), después debe iniciar sesión desde el login. */
    suspend fun registrar(nombre: String, correo: String, contrasena: String): Resultado<UsuarioRespuesta> =
        llamarApi { api.registrar(RegistroPeticion(nombre.trim(), correo.trim(), contrasena)) }
}
