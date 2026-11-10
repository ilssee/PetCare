package com.petcare.app.mapa

/** Qué categoría se está viendo en el mapa. Si no hay ninguna, se muestran todos los marcadores. */
enum class Categoria { VETERINARIAS, CAMPANAS }

/** Filtros elegidos por el usuario para veterinarias. Un conjunto vacío significa "no filtrar por eso". */
data class FiltrosVeterinarias(
    val tipos: Set<String> = emptySet(),
    val especies: Set<String> = emptySet(),
    val servicios: Set<Int> = emptySet(),
    val disponibleAhora: Boolean = false,
) {
    /** Cuántos filtros hay activos (para el contador del botón Filtros). */
    val cantidad get() = tipos.size + especies.size + servicios.size + if (disponibleAhora) 1 else 0
}

/** Filtros elegidos por el usuario para campañas. */
data class FiltrosCampanas(
    val estados: Set<String> = emptySet(),
    val especies: Set<String> = emptySet(),
    val servicios: Set<Int> = emptySet(),
    val costos: Set<String> = emptySet(),
    val disponibleAhora: Boolean = false,
) {
    val cantidad get() = estados.size + especies.size + servicios.size + costos.size + if (disponibleAhora) 1 else 0
}
