package com.petcare.app.cercania

/**
 * Configuración de avisos de cercanía (ver backend/src/modules/cercania). Sirve para leerla y para guardarla.
 * La ubicación y el radio son opcionales; sin ellos no se puede activar ningún aviso.
 */
data class ConfiguracionCercania(
    val lat: Double?,
    val lng: Double?,
    val radioKm: Double?,
    val notificarCampanas: Boolean,
    val notificarVeterinarias: Boolean,
)
