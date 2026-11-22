package com.petcare.app.cuidado

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import com.petcare.app.MainActivity
import com.petcare.app.R
import com.petcare.app.databinding.ItemCronogramaBinding
import com.petcare.app.mascota.Especies
import com.petcare.app.mascota.MascotaRespuesta
import com.petcare.app.mascota.mostrarMascota

/**
 * Fila de un cuidado pendiente: la mascota, el nombre, "Mascota · frecuencia" y la fecha a la derecha
 * (si ya venció: tarjeta con borde rojo suave y "Venció hace N días" como etiqueta roja). La usan el Cronograma y el perfil de la mascota.
 */
fun ItemCronogramaBinding.mostrarCuidado(c: CuidadoRespuesta, mascota: MascotaRespuesta?) {
    val contexto = root.context
    if (mascota != null) {
        fotoMascota.mostrarMascota(mascota)
        fotoMascota.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(contexto, Especies.color(mascota.especie)))
    }
    nombre.text = c.nombre
    detalle.text = contexto.getString(R.string.cronograma_mascota_frecuencia, mascota?.nombre.orEmpty(), c.textoFrecuencia(contexto))

    val vencido = c.diasVencido()
    if (vencido > 0) {
        fecha.text = contexto.resources.getQuantityString(R.plurals.cuidado_vencio, vencido.toInt(), vencido.toInt())
        // Vencido: tarjeta con borde rojo suave y "Venció hace N días" como etiqueta roja, igual que en Cuidados.
        tarjeta.setBackgroundResource(R.drawable.bg_tarjeta_vencida)
        val densidad = contexto.resources.displayMetrics.density
        fecha.setBackgroundResource(R.drawable.bg_pastilla)
        fecha.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(contexto, R.color.petcare_danger_100))
        fecha.setPadding((10 * densidad).toInt(), (3 * densidad).toInt(), (10 * densidad).toInt(), (3 * densidad).toInt())
        fecha.setTextColor(ContextCompat.getColor(contexto, R.color.petcare_error))
    } else {
        fecha.text = c.fechaRecurrente.comoFechaCorta()
        tarjeta.setBackgroundResource(R.drawable.bg_tarjeta_suave)
        fecha.background = null
        fecha.backgroundTintList = null
        fecha.setPadding(0, 0, 0, 0)
        fecha.setTextColor(ContextCompat.getColor(contexto, R.color.petcare_text))
    }
}

/**
 * Abre la pestaña Cuidados con la mascota indicada elegida (aunque esté inactiva).
 * Si se indica [cuidadoId], ese cuidado se abre ya desplegado.
 */
fun Context.abrirCuidadosDe(mascotaId: Int, cuidadoId: Int = 0) {
    SeleccionCuidados.mascotaId = mascotaId
    SeleccionCuidados.cuidadoId = cuidadoId
    startActivity(
        Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(MainActivity.EXTRA_SECCION, R.id.nav_cuidados),
    )
}
