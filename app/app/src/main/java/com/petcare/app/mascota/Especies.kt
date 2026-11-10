package com.petcare.app.mascota

import android.widget.ImageView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import coil.load
import coil.transform.CircleCropTransformation
import com.petcare.app.BuildConfig
import com.petcare.app.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Cómo se ve cada especie en la app: color de fondo, ilustración y nombre. */
object Especies {
    @ColorRes
    fun color(especie: String) = when (especie) {
        "gato" -> R.color.especie_gato
        "conejo" -> R.color.especie_conejo
        else -> R.color.especie_perro
    }

    @DrawableRes
    fun ilustracion(especie: String) = when (especie) {
        "gato" -> R.drawable.ilustracion_gato
        "conejo" -> R.drawable.ilustracion_conejo
        else -> R.drawable.ilustracion_perro
    }

    @StringRes
    fun nombre(especie: String) = when (especie) {
        "gato" -> R.string.especie_gato
        "conejo" -> R.string.especie_conejo
        else -> R.string.especie_perro
    }
}

/** "hembra" -> "Hembra" */
fun String.conPrimeraMayuscula() = replaceFirstChar { it.uppercase() }

/** "25.0" -> "25", "24.5" -> "24.5" */
fun Double.comoPeso(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()

/** "2023-08-05" -> "5 ago 2023" */
fun String.comoFechaLegible(): String = runCatching {
    LocalDate.parse(this).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es", "PE")))
}.getOrDefault(this)

/** Dirección pública de una foto guardada en el servidor ("mascotas/xxxx.jpg"). */
fun String.comoUrlFoto(): String = BuildConfig.API_BASE_URL + "uploads/" + this

/** Muestra la foto de la mascota si tiene; si no, la ilustración de su especie. */
fun ImageView.mostrarMascota(mascota: MascotaRespuesta) {
    val ilustracion = Especies.ilustracion(mascota.especie)
    val foto = mascota.foto
    // Se recuerda el relleno original: la ilustración lo necesita, la foto no (debe ocupar todo el círculo).
    val relleno = (getTag() as? IntArray) ?: intArrayOf(paddingLeft, paddingTop, paddingRight, paddingBottom).also { setTag(it) }
    if (foto.isNullOrEmpty()) {
        setPadding(relleno[0], relleno[1], relleno[2], relleno[3])
        scaleType = ImageView.ScaleType.FIT_CENTER
        setImageResource(ilustracion)
    } else {
        setPadding(0, 0, 0, 0)
        scaleType = ImageView.ScaleType.CENTER_CROP
        load(foto.comoUrlFoto()) {
            transformations(CircleCropTransformation())
            placeholder(ilustracion)
            error(ilustracion)
        }
    }
}
