package com.petcare.app.mapa

/** Datos que devuelve la API del mapa (ver backend/src/modules/mapa). */

data class ServicioRespuesta(val id: Int, val nombre: String)

data class CampanaAqui(val id: Int, val estado: String)

/** Franja de atención de hoy: horas "HH:MM:SS". */
data class FranjaHoy(val apertura: String, val cierre: String)

data class VeterinariaRespuesta(
    val id: Int,
    val nombre: String,
    /** "municipal" o "privada" */
    val tipo: String,
    val lat: Double,
    val lng: Double,
    val promedio: Double?,
    val totalResenas: Int,
    val distanciaKm: Double?,
    val abiertaAhora: Boolean,
    /** Franjas de atención de hoy; vacía si hoy no atiende. */
    val horarioHoy: List<FranjaHoy>,
    val especies: List<String>,
    val servicios: List<ServicioRespuesta>,
    /** Campañas vigentes de esta veterinaria que están en su misma ubicación (se dibujan juntas). */
    val campanasAqui: List<CampanaAqui>,
)

data class CampanaRespuesta(
    val id: Int,
    val nombre: String,
    val organizador: String,
    /** "proxima" o "activa" */
    val estado: String,
    val lat: Double,
    val lng: Double,
    val fechaInicio: String,
    val fechaFin: String,
    val horaApertura: String,
    val horaCierre: String,
    /** "gratuita" o "de_pago" */
    val costo: String,
    /** Calificación promedio y total de reseñas del organizador. */
    val promedio: Double?,
    val totalResenas: Int,
    val distanciaKm: Double?,
    val disponibleAhora: Boolean,
    val especies: List<String>,
    val servicios: List<ServicioRespuesta>,
    val veterinariaId: Int?,
    /** La campaña está en la misma ubicación de su veterinaria: se muestra junto a ella. */
    val enUbicacionDeVeterinaria: Boolean,
    /** Solo llegan dentro del perfil de una veterinaria, para desplegar la campaña (RF43). */
    val descripcion: String? = null,
    val direccion: String? = null,
)

/** Quien organiza la campaña, para el resumen del detalle y para abrir sus reseñas. */
data class OrganizadorResumen(val id: Int, val nombre: String, val promedio: Double?, val totalResenas: Int)

/** Detalle de una campaña (RF43), con el resumen de su organizador. */
data class DetalleCampanaRespuesta(
    val id: Int,
    val nombre: String,
    val organizador: String,
    /** "proxima" o "activa" */
    val estado: String,
    val fechaInicio: String,
    val fechaFin: String,
    val horaApertura: String,
    val horaCierre: String,
    /** "gratuita" o "de_pago" */
    val costo: String,
    val descripcion: String,
    val direccion: String?,
    val especies: List<String>,
    val servicios: List<ServicioRespuesta>,
    val veterinariaId: Int?,
    val organizadorDetalle: OrganizadorResumen,
)

/** Resultado de buscar por nombre: "clase" es "veterinaria" o "campana"; solo viene lleno uno de los dos grupos. */
data class ResultadoBusquedaRespuesta(
    val clase: String,
    val id: Int,
    val nombre: String,
    val lat: Double,
    val lng: Double,
    val distanciaKm: Double?,
    val promedio: Double?,
    val especies: List<String>?,
    val servicios: List<ServicioRespuesta>?,
    // Solo veterinarias
    val tipo: String?,
    val abiertaAhora: Boolean?,
    val totalResenas: Int?,
    val horarioHoy: List<FranjaHoy>?,
    val campanasAqui: List<CampanaAqui>?,
    // Solo campañas
    val organizador: String?,
    val estado: String?,
    val fechaInicio: String?,
    val fechaFin: String?,
    val horaApertura: String?,
    val horaCierre: String?,
    val costo: String?,
    val disponibleAhora: Boolean?,
    val veterinariaId: Int?,
    val enUbicacionDeVeterinaria: Boolean?,
)

data class ServicioCatalogo(val id: Int, val nombre: String, val categoria: String)

data class CatalogosRespuesta(val especies: List<String>, val servicios: List<ServicioCatalogo>)

/** Un elemento del mapa ya unificado para dibujarlo y mostrarlo en la lista, sea veterinaria o campaña. */
sealed class ElementoMapa {
    abstract val id: Int
    abstract val nombre: String
    abstract val lat: Double
    abstract val lng: Double
    abstract val distanciaKm: Double?
    abstract val promedio: Double?

    data class Veterinaria(val datos: VeterinariaRespuesta) : ElementoMapa() {
        override val id get() = datos.id
        override val nombre get() = datos.nombre
        override val lat get() = datos.lat
        override val lng get() = datos.lng
        override val distanciaKm get() = datos.distanciaKm
        override val promedio get() = datos.promedio
    }

    data class Campana(val datos: CampanaRespuesta) : ElementoMapa() {
        override val id get() = datos.id
        override val nombre get() = datos.nombre
        override val lat get() = datos.lat
        override val lng get() = datos.lng
        override val distanciaKm get() = datos.distanciaKm
        override val promedio get() = datos.promedio
    }
}

/** Convierte un resultado de búsqueda en un elemento del mapa. */
fun ResultadoBusquedaRespuesta.comoElemento(): ElementoMapa =
    if (clase == "veterinaria") {
        ElementoMapa.Veterinaria(
            VeterinariaRespuesta(
                id = id,
                nombre = nombre,
                tipo = tipo ?: "privada",
                lat = lat,
                lng = lng,
                promedio = promedio,
                totalResenas = totalResenas ?: 0,
                distanciaKm = distanciaKm,
                abiertaAhora = abiertaAhora ?: false,
                horarioHoy = horarioHoy.orEmpty(),
                especies = especies.orEmpty(),
                servicios = servicios.orEmpty(),
                campanasAqui = campanasAqui.orEmpty(),
            ),
        )
    } else {
        ElementoMapa.Campana(
            CampanaRespuesta(
                id = id,
                nombre = nombre,
                organizador = organizador.orEmpty(),
                estado = estado ?: "activa",
                lat = lat,
                lng = lng,
                fechaInicio = fechaInicio.orEmpty(),
                fechaFin = fechaFin.orEmpty(),
                horaApertura = horaApertura.orEmpty(),
                horaCierre = horaCierre.orEmpty(),
                costo = costo ?: "gratuita",
                promedio = promedio,
                totalResenas = totalResenas ?: 0,
                distanciaKm = distanciaKm,
                disponibleAhora = disponibleAhora ?: false,
                especies = especies.orEmpty(),
                servicios = servicios.orEmpty(),
                veterinariaId = veterinariaId,
                enUbicacionDeVeterinaria = enUbicacionDeVeterinaria ?: false,
            ),
        )
    }
