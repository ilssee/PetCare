package com.petcare.app.common

import android.content.Context
import android.content.SharedPreferences

/**
 * Guarda la sesión del usuario (token y nombre) en el dispositivo.
 * Es un singleton: existe una sola instancia que usa toda la app.
 */
object SessionManager {
    private const val ARCHIVO = "sesion_petcare"
    private const val CLAVE_TOKEN = "token"
    private const val CLAVE_NOMBRE = "nombre"

    private lateinit var preferencias: SharedPreferences

    fun iniciar(contexto: Context) {
        preferencias = contexto.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
    }

    val token: String? get() = preferencias.getString(CLAVE_TOKEN, null)
    val nombre: String? get() = preferencias.getString(CLAVE_NOMBRE, null)
    val haySesion: Boolean get() = !token.isNullOrEmpty()

    fun guardar(token: String, nombre: String) {
        preferencias.edit().putString(CLAVE_TOKEN, token).putString(CLAVE_NOMBRE, nombre).apply()
    }

    fun guardarNombre(nombre: String) {
        preferencias.edit().putString(CLAVE_NOMBRE, nombre).apply()
    }

    fun cerrar() {
        preferencias.edit().clear().apply()
    }
}
