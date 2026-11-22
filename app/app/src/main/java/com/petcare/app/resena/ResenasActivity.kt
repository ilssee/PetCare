package com.petcare.app.resena

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.petcare.app.R
import com.petcare.app.common.Resultado
import com.petcare.app.common.paraMostrar
import com.petcare.app.common.respetarBarrasYTeclado
import com.petcare.app.databinding.ActivityResenasBinding
import com.petcare.app.mapa.FormatosMapa
import com.petcare.app.mapa.ResenaRespuesta
import kotlinx.coroutines.launch

/**
 * Todas las reseñas de una veterinaria (RF44), ordenables por más recientes o mejor calificadas.
 * El usuario puede editar o eliminar las suyas (RF46) y escribir una nueva (RF45).
 */
class ResenasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResenasBinding
    private val repositorio = ResenaRepository()
    private var orden = "recientes"

    private val organizadorId get() = intent.getIntExtra(EXTRA_ID, 0)
    private val nombreDelOrganizador get() = intent.getStringExtra(EXTRA_NOMBRE).orEmpty()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityResenasBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.raiz.respetarBarrasYTeclado()

        binding.nombre.text = nombreDelOrganizador
        binding.botonVolver.setOnClickListener { finish() }
        binding.botonEscribir.setOnClickListener {
            startActivity(EscribirResenaActivity.intentNueva(this, organizadorId, nombreDelOrganizador))
        }
        binding.ordenRecientes.setOnClickListener { cambiarOrden("recientes") }
        binding.ordenCalificadas.setOnClickListener { cambiarOrden("calificadas") }
        pintarOrden()
    }

    // Al volver de escribir o editar una reseña, la lista se actualiza sola.
    override fun onResume() {
        super.onResume()
        cargar()
    }

    private fun cambiarOrden(nuevo: String) {
        if (nuevo == orden) return
        orden = nuevo
        pintarOrden()
        cargar()
    }

    /** El orden elegido se ve con borde y fondo verde azulado claro. */
    private fun pintarOrden() {
        val recientes = orden == "recientes"
        pintarOpcion(binding.ordenRecientes, recientes)
        pintarOpcion(binding.ordenCalificadas, !recientes)
    }

    private fun pintarOpcion(vista: android.widget.TextView, activa: Boolean) {
        vista.setBackgroundResource(if (activa) R.drawable.bg_chip_opcion_activo else R.drawable.bg_chip_opcion)
        vista.setTextColor(ContextCompat.getColor(this, if (activa) R.color.petcare_text else R.color.petcare_text_muted))
    }

    private fun cargar() {
        lifecycleScope.launch {
            when (val resultado = repositorio.listar(organizadorId, orden)) {
                is Resultado.Exito -> {
                    binding.textoError.isVisible = false
                    pintar(resultado.dato)
                }
                is Resultado.Fallo -> mostrarError(resultado.paraMostrar(this@ResenasActivity))
            }
        }
    }

    private fun pintar(listado: ListadoResenasRespuesta) {
        val hay = listado.total > 0
        binding.resumen.isVisible = hay
        binding.ordenes.isVisible = hay
        binding.mensajeVacio.isVisible = !hay
        if (hay) {
            binding.promedio.text = FormatosMapa.calificacion(listado.promedio ?: 0.0)
            FormatosMapa.pintarEstrellas(binding.estrellasResumen, FormatosMapa.estrellasDe(listado.promedio), 16)
            binding.totalResenas.text =
                if (listado.total == 1) getString(R.string.perfil_una_resena) else getString(R.string.perfil_varias_resenas, listado.total)
        }
        binding.lista.removeAllViews()
        for (resena in listado.resenas) {
            binding.lista.addView(ResenaFila.crear(binding.lista, resena, alEditar = ::editar, alEliminar = ::confirmarEliminacion))
        }
    }

    private fun editar(resena: ResenaRespuesta) {
        startActivity(EscribirResenaActivity.intentEdicion(this, organizadorId, nombreDelOrganizador, resena))
    }

    private fun confirmarEliminacion(resena: ResenaRespuesta) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.resenas_eliminar_titulo)
            .setMessage(R.string.resenas_eliminar_mensaje)
            .setPositiveButton(R.string.resenas_eliminar) { _, _ -> eliminar(resena) }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    private fun eliminar(resena: ResenaRespuesta) {
        lifecycleScope.launch {
            when (val resultado = repositorio.eliminar(resena.id)) {
                is Resultado.Exito -> {
                    Toast.makeText(this@ResenasActivity, R.string.resenas_eliminada, Toast.LENGTH_SHORT).show()
                    cargar()
                }
                // Si no se pudo, la información queda como estaba y se avisa (RF46).
                is Resultado.Fallo -> mostrarError(resultado.paraMostrar(this@ResenasActivity))
            }
        }
    }

    private fun mostrarError(mensaje: String) {
        binding.textoError.text = mensaje
        binding.textoError.isVisible = true
    }

    companion object {
        private const val EXTRA_ID = "id"
        private const val EXTRA_NOMBRE = "nombre"

        fun intent(contexto: Context, organizadorId: Int, nombre: String): Intent =
            Intent(contexto, ResenasActivity::class.java).putExtra(EXTRA_ID, organizadorId).putExtra(EXTRA_NOMBRE, nombre)
    }
}
