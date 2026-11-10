package com.petcare.app.resena

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.petcare.app.R
import com.petcare.app.common.Resultado
import com.petcare.app.common.paraMostrar
import com.petcare.app.common.respetarBarrasYTeclado
import com.petcare.app.databinding.ActivityEscribirResenaBinding
import com.petcare.app.mapa.ResenaRespuesta
import kotlinx.coroutines.launch

/**
 * Escribir una reseña (RF45) o editar una propia (RF46): una calificación obligatoria de 1 a 5 y un comentario
 * opcional. Para editar se abre con la reseña ya cargada.
 */
class EscribirResenaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEscribirResenaBinding
    private val repositorio = ResenaRepository()
    private var calificacion = 0

    private val organizadorId get() = intent.getIntExtra(EXTRA_ORGANIZADOR, 0)
    private val resenaId get() = intent.getIntExtra(EXTRA_RESENA, 0) // 0 al escribir una nueva
    private val editando get() = resenaId != 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEscribirResenaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.raiz.respetarBarrasYTeclado()

        binding.nombre.text = intent.getStringExtra(EXTRA_NOMBRE).orEmpty()
        binding.titulo.setText(if (editando) R.string.resenas_editar_titulo else R.string.resenas_escribir_titulo)
        binding.botonEnviar.setText(if (editando) R.string.resenas_guardar_cambios else R.string.resenas_enviar)
        binding.botonVolver.setOnClickListener { finish() }
        binding.botonEnviar.setOnClickListener { enviar() }

        if (savedInstanceState == null && editando) {
            calificacion = intent.getIntExtra(EXTRA_CALIFICACION, 0)
            binding.campoComentario.setText(intent.getStringExtra(EXTRA_COMENTARIO).orEmpty())
        } else {
            calificacion = savedInstanceState?.getInt(ESTADO_CALIFICACION) ?: 0
        }
        crearEstrellas()
    }

    override fun onSaveInstanceState(estado: Bundle) {
        super.onSaveInstanceState(estado)
        estado.putInt(ESTADO_CALIFICACION, calificacion)
    }

    /** Cinco estrellas grandes: al tocar la n-ésima, la calificación es n y se pintan las n primeras. */
    private fun crearEstrellas() {
        val lado = (46 * resources.displayMetrics.density).toInt()
        val separacion = (6 * resources.displayMetrics.density).toInt()
        binding.estrellas.removeAllViews()
        for (n in 1..5) {
            binding.estrellas.addView(
                ImageView(this).apply {
                    contentDescription = getString(R.string.resenas_descripcion_estrella, n)
                    layoutParams = LinearLayout.LayoutParams(lado, lado).apply { marginStart = if (n == 1) 0 else separacion }
                    setPadding(separacion / 2, separacion / 2, separacion / 2, separacion / 2)
                    setOnClickListener {
                        calificacion = n
                        binding.textoError.isVisible = false
                        pintarEstrellas()
                    }
                },
            )
        }
        pintarEstrellas()
    }

    private fun pintarEstrellas() {
        for (n in 1..5) {
            (binding.estrellas.getChildAt(n - 1) as ImageView)
                .setImageResource(if (n <= calificacion) R.drawable.ic_estrella_llena else R.drawable.ic_estrella_vacia)
        }
    }

    private fun enviar() {
        // Sin calificación no se puede guardar (RF45).
        if (calificacion == 0) {
            mostrarError(getString(R.string.resenas_sin_calificacion))
            return
        }
        val comentario = binding.campoComentario.text.toString().trim().ifEmpty { null }
        binding.textoError.isVisible = false
        binding.botonEnviar.isEnabled = false
        lifecycleScope.launch {
            val resultado = if (editando) {
                repositorio.editar(resenaId, calificacion, comentario)
            } else {
                repositorio.crear(organizadorId, calificacion, comentario)
            }
            when (resultado) {
                is Resultado.Exito -> {
                    Toast.makeText(
                        this@EscribirResenaActivity,
                        if (editando) R.string.resenas_actualizada else R.string.resenas_enviada,
                        Toast.LENGTH_SHORT,
                    ).show()
                    finish()
                }
                is Resultado.Fallo -> {
                    mostrarError(resultado.paraMostrar(this@EscribirResenaActivity))
                    binding.botonEnviar.isEnabled = true
                }
            }
        }
    }

    private fun mostrarError(mensaje: String) {
        binding.textoError.text = mensaje
        binding.textoError.isVisible = true
    }

    companion object {
        private const val EXTRA_ORGANIZADOR = "organizador"
        private const val EXTRA_NOMBRE = "nombre"
        private const val EXTRA_RESENA = "resena"
        private const val EXTRA_CALIFICACION = "calificacion"
        private const val EXTRA_COMENTARIO = "comentario"
        private const val ESTADO_CALIFICACION = "estado_calificacion"

        fun intentNueva(contexto: Context, organizadorId: Int, nombre: String): Intent =
            Intent(contexto, EscribirResenaActivity::class.java)
                .putExtra(EXTRA_ORGANIZADOR, organizadorId)
                .putExtra(EXTRA_NOMBRE, nombre)

        fun intentEdicion(contexto: Context, organizadorId: Int, nombre: String, resena: ResenaRespuesta): Intent =
            intentNueva(contexto, organizadorId, nombre)
                .putExtra(EXTRA_RESENA, resena.id)
                .putExtra(EXTRA_CALIFICACION, resena.calificacion)
                .putExtra(EXTRA_COMENTARIO, resena.comentario)
    }
}
