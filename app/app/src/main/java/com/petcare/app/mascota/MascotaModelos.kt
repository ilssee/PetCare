package com.petcare.app.mascota

/** Datos que se envían y reciben de la API (módulo mascota). */

/** Raza, fecha y peso son opcionales. Para crear se envía la especie; al editar se omite (no se puede cambiar). */
data class MascotaPeticion(
    val nombre: String,
    val sexo: String,
    val raza: String? = null,
    val fechaNacimiento: String? = null,
    val peso: Double? = null,
    val especie: String? = null,
)

data class MascotaRespuesta(
    val id: Int,
    val nombre: String,
    val especie: String,
    val raza: String?,
    val fechaNacimiento: String?,
    val sexo: String,
    val peso: Double?,
    val foto: String?,
    val estado: String,
    val edadTexto: String?,
) {
    val estaInactiva: Boolean get() = estado == "inactiva"
}
