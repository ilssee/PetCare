package com.petcare.app.resena

/**
 * Aviso interno de la app: se marca cuando el usuario guarda, edita o elimina una reseña, para que el mapa
 * sepa que debe volver a cargar las calificaciones al regresar a él. Solo vive en la memoria del celular.
 */
object ResenasCambiaron {
    @Volatile
    private var hayCambios = false

    fun marcar() {
        hayCambios = true
    }

    /** Dice si hubo cambios desde la última vez y deja el aviso desmarcado. */
    fun consumir(): Boolean {
        val resultado = hayCambios
        hayCambios = false
        return resultado
    }
}
