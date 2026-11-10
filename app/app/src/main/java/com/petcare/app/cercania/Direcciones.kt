package com.petcare.app.cercania

import com.petcare.app.BuildConfig
import okhttp3.OkHttpClient
import org.osmdroid.util.GeoPoint
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.IOException

/**
 * Búsqueda de direcciones con autocompletado (RF37), con Photon, un servicio gratuito basado en datos de
 * OpenStreetMap. No lleva clave. Se usa un cliente aparte del de la API de PetCare para no enviar la sesión
 * del usuario a un tercero: solo viajan el texto que escribe y, al tocar el mapa, las coordenadas.
 */

data class DireccionRespuesta(val features: List<DireccionLugar>)

data class DireccionLugar(val geometry: DireccionGeometria?, val properties: DireccionPropiedades?)

/** Photon entrega las coordenadas como [longitud, latitud]. */
data class DireccionGeometria(val coordinates: List<Double>)

data class DireccionPropiedades(
    val name: String?,
    val street: String?,
    val housenumber: String?,
    val district: String?,
    val city: String?,
)

interface DireccionesApi {
    @GET("api/")
    suspend fun buscar(
        @Query("q") texto: String,
        @Query("limit") limite: Int = 6,
        @Query("lat") lat: Double = LIMA.latitude, // da prioridad a los resultados cercanos a Lima
        @Query("lon") lng: Double = LIMA.longitude,
    ): DireccionRespuesta

    @GET("reverse")
    suspend fun alReves(@Query("lat") lat: Double, @Query("lon") lng: Double): DireccionRespuesta

    companion object {
        val LIMA = GeoPoint(-12.0464, -77.0428)
    }
}

/** Una dirección sugerida: el texto para mostrar y su punto en el mapa. */
data class Direccion(val texto: String, val punto: GeoPoint)

object Direcciones {
    private val api: DireccionesApi by lazy {
        val cliente = OkHttpClient.Builder()
            .addInterceptor { cadena ->
                // El servicio pide identificar la aplicación que lo usa.
                cadena.proceed(cadena.request().newBuilder().header("User-Agent", BuildConfig.APPLICATION_ID).build())
            }
            .build()
        Retrofit.Builder()
            .baseUrl("https://photon.komoot.io/")
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DireccionesApi::class.java)
    }

    /** Sugerencias para lo que el usuario escribió. Si no hay conexión o el servicio falla, devuelve una lista vacía. */
    suspend fun buscar(texto: String): List<Direccion> = try {
        api.buscar(texto).features.mapNotNull { it.comoDireccion() }.distinctBy { it.texto }
    } catch (e: IOException) {
        emptyList()
    } catch (e: RuntimeException) {
        emptyList()
    }

    /** La dirección que corresponde a un punto del mapa, o null si no se pudo averiguar. */
    suspend fun deUnPunto(punto: GeoPoint): Direccion? = try {
        api.alReves(punto.latitude, punto.longitude).features.firstNotNullOfOrNull { it.comoDireccion() }
    } catch (e: IOException) {
        null
    } catch (e: RuntimeException) {
        null
    }

    private fun DireccionLugar.comoDireccion(): Direccion? {
        val coordenadas = geometry?.coordinates ?: return null
        if (coordenadas.size < 2) return null
        val p = properties
        val calle = listOfNotNull(p?.street, p?.housenumber).joinToString(" ").ifBlank { null }
        val texto = listOfNotNull(p?.name, calle, p?.district, p?.city)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(", ")
        if (texto.isBlank()) return null
        return Direccion(texto, GeoPoint(coordenadas[1], coordenadas[0]))
    }
}
