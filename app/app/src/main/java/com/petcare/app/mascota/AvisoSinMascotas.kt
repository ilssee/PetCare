package com.petcare.app.mascota

import com.petcare.app.R
import com.petcare.app.common.configurar
import com.petcare.app.databinding.VistaEstadoVacioBinding

/**
 * Mensaje que se muestra en Mascotas, Cronograma y Cuidados cuando falta una mascota.
 * Un mismo estado dice lo mismo en todas las pantallas:
 *  - ninguna mascota:                 "Aún no tienes mascotas"
 *  - mascotas, pero todas inactivas:  "Sin mascotas activas"
 * Devuelve true si hay que mostrar el aviso (y lo deja configurado), o false si hay mascotas activas.
 */
fun VistaEstadoVacioBinding.configurarSiNoHayMascotas(
    mascotas: List<MascotaRespuesta>,
    alAgregar: () -> Unit,
): Boolean = when {
    mascotas.isEmpty() -> {
        configurar(R.drawable.ic_huella, R.string.mascotas_vacio_titulo, R.string.mascotas_vacio_mensaje, alTocarBoton = alAgregar)
        true
    }
    mascotas.none { !it.estaInactiva } -> {
        configurar(R.drawable.ic_huella, R.string.sin_activas_titulo, R.string.sin_activas_mensaje, alTocarBoton = alAgregar)
        true
    }
    else -> false
}
