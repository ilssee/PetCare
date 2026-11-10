package com.petcare.app.mapa

/** Datos que devuelve la API para el perfil de una veterinaria (ver backend/src/modules/mapa/mapa.perfil.ts). */

/** Días seguidos con el mismo horario. Sin franjas significa que ese grupo de días no atiende. */
data class GrupoHorario(
    val desde: String,
    val hasta: String,
    val franjas: List<FranjaHoy>,
    val incluyeHoy: Boolean,
)

data class ServiciosDeCategoria(val categoria: String, val servicios: List<String>)

/** Una reseña tal como la entrega el servidor. [esMia] marca las del usuario actual, que se pueden editar o eliminar. */
data class ResenaRespuesta(
    val id: Int,
    val autor: String,
    val calificacion: Int,
    val comentario: String?,
    /** AAAA-MM-DD */
    val fecha: String,
    val esMia: Boolean,
)

data class ResenasDelPerfil(val promedio: Double?, val total: Int, val recientes: List<ResenaRespuesta>)

data class PerfilVeterinariaRespuesta(
    val id: Int,
    val nombre: String,
    /** "municipal" o "privada" */
    val tipo: String,
    val lat: Double,
    val lng: Double,
    /** Dirección escrita, por ejemplo "Av. Los Olivos 123, Lima"; puede no estar registrada. */
    val direccion: String?,
    val promedio: Double?,
    val totalResenas: Int,
    val distanciaKm: Double?,
    val abiertaAhora: Boolean,
    val horarioHoy: List<FranjaHoy>,
    val especies: List<String>,
    val horarioSemana: List<GrupoHorario>,
    val serviciosPorCategoria: List<ServiciosDeCategoria>,
    val telefonos: List<String>,
    val redes: List<String>,
    /** Rutas de las fotos guardadas en el servidor ("veterinarias/xxxx.jpg"). */
    val fotos: List<String>,
    val campanas: List<CampanaRespuesta>,
    val resenas: ResenasDelPerfil,
)
