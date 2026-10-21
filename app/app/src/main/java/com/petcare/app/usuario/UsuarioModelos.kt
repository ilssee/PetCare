package com.petcare.app.usuario

/** Datos que se envían y reciben de la API (módulo usuario). */

data class RegistroPeticion(val nombre: String, val correo: String, val contrasena: String)

data class LoginPeticion(val correo: String, val contrasena: String)

data class UsuarioRespuesta(val id: Int, val nombre: String, val correo: String)

data class LoginRespuesta(val token: String, val usuario: UsuarioRespuesta)
