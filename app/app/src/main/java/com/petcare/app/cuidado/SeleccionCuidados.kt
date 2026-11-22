package com.petcare.app.cuidado

/**
 * Mascota que la pestaña Cuidados debe mostrar la próxima vez que se abra.
 * Las pantallas que activan o agregan cuidados la dejan aquí para que el usuario vea el resultado
 * (0 significa que no hay ninguna en particular).
 */
object SeleccionCuidados {
    var mascotaId = 0

    /** Cuidado que debe abrirse ya desplegado (0 = ninguno); lo deja el Cronograma al tocar un cuidado. */
    var cuidadoId = 0
}
