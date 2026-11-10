package com.petcare.app.mapa

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.petcare.app.R
import com.petcare.app.common.Resultado
import com.petcare.app.common.respetarBarrasYTeclado
import com.petcare.app.databinding.ActivityCampanaDetalleBinding
import com.petcare.app.resena.ResenasActivity
import kotlinx.coroutines.launch

/**
 * Detalle de una campaña organizada por una entidad externa (RF43): sus datos y el resumen de su organizador,
 * con acceso a sus reseñas. Las campañas de una veterinaria se ven dentro del perfil de esa veterinaria.
 */
class CampanaDetalleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCampanaDetalleBinding
    private val repositorio = MapaRepository()

    private val campanaId get() = intent.getIntExtra(EXTRA_ID, 0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCampanaDetalleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.raiz.respetarBarrasYTeclado()
        binding.botonVolver.setOnClickListener { finish() }
    }

    // Al volver de escribir, editar o eliminar una reseña del organizador, su calificación se actualiza sola.
    override fun onResume() {
        super.onResume()
        cargar()
    }

    private fun cargar() {
        val lat = if (intent.hasExtra(EXTRA_LAT)) intent.getDoubleExtra(EXTRA_LAT, 0.0) else null
        val lng = if (intent.hasExtra(EXTRA_LNG)) intent.getDoubleExtra(EXTRA_LNG, 0.0) else null
        lifecycleScope.launch {
            when (val resultado = repositorio.detalleDeCampana(campanaId, lat, lng)) {
                is Resultado.Exito -> pintar(resultado.dato)
                // La campaña pudo terminar o no recuperarse: se avisa que no se puede ver su detalle.
                is Resultado.Fallo -> {
                    binding.desplazamiento.isVisible = false
                    binding.mensajeError.isVisible = true
                    binding.mensajeError.setText(R.string.campana_no_cargo)
                }
            }
        }
    }

    private fun pintar(d: DetalleCampanaRespuesta) {
        binding.contenido.visibility = View.VISIBLE
        binding.nombre.text = d.nombre
        binding.ilustracion.backgroundTintList = ColorStateList.valueOf(color(MarcadoresMapa.colorDeCampana(d.estado)))

        val activa = d.estado == "activa"
        binding.etiquetaEstado.setText(if (activa) R.string.mapa_estado_activa else R.string.mapa_estado_proxima)
        binding.etiquetaEstado.backgroundTintList =
            ColorStateList.valueOf(color(if (activa) R.color.etiqueta_activa_fondo else R.color.etiqueta_proxima_fondo))
        binding.etiquetaEstado.setTextColor(color(if (activa) R.color.etiqueta_activa_texto else R.color.etiqueta_proxima_texto))
        binding.etiquetaCosto.setText(if (d.costo == "gratuita") R.string.mapa_costo_gratuita else R.string.mapa_costo_de_pago)
        binding.etiquetaCosto.backgroundTintList = ColorStateList.valueOf(color(R.color.petcare_field))

        val organizador = d.organizadorDetalle
        binding.organizador.text = organizador.nombre
        binding.calificacion.text = organizador.promedio?.let { FormatosMapa.calificacion(it) } ?: getString(R.string.mapa_sin_resenas)

        binding.cuerpo.removeAllViews()
        binding.cuerpo.addView(CuerpoCampana.crear(this, d.comoCuerpo()))

        binding.verResenas.text = getString(R.string.campana_ver_resenas, organizador.nombre)
        binding.verResenas.setOnClickListener {
            startActivity(ResenasActivity.intent(this, organizador.id, organizador.nombre))
        }
    }

    private fun color(id: Int) = ContextCompat.getColor(this, id)

    companion object {
        private const val EXTRA_ID = "id"
        private const val EXTRA_LAT = "lat"
        private const val EXTRA_LNG = "lng"

        /** [lat] y [lng] son la ubicación del usuario, si se conoce. */
        fun intent(contexto: Context, id: Int, lat: Double?, lng: Double?): Intent =
            Intent(contexto, CampanaDetalleActivity::class.java).apply {
                putExtra(EXTRA_ID, id)
                if (lat != null && lng != null) {
                    putExtra(EXTRA_LAT, lat)
                    putExtra(EXTRA_LNG, lng)
                }
            }
    }
}
