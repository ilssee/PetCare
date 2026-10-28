package com.petcare.app.cuidado

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.petcare.app.R
import com.petcare.app.common.respetarBarrasYTeclado
import com.petcare.app.common.Resultado
import com.petcare.app.common.paraMostrar
import com.petcare.app.databinding.ActivityCuidadoFormBinding
import com.petcare.app.mascota.MascotaRepository
import com.petcare.app.mascota.mostrarMascota
import kotlinx.coroutines.launch

/**
 * Formulario de un cuidado, con tres usos:
 *  - agregar un cuidado personalizado (RF12): solo recibe la mascota;
 *  - editar un cuidado existente (RF14): recibe EXTRA_CUIDADO_ID;
 *  - agregar una recomendación al cronograma (RF25): recibe la mascota y los datos de la recomendación.
 */
class CuidadoFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCuidadoFormBinding
    private lateinit var formulario: FormularioCuidado
    private val mascotas = MascotaRepository()
    private val repositorio = CuidadoRepository()

    private var mascotaId = 0
    private var cuidadoId = 0
    private var cuidado: CuidadoRespuesta? = null
    private var recomendacion: RecomendacionRespuesta? = null

    private val editando get() = cuidadoId != 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCuidadoFormBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.raiz.respetarBarrasYTeclado()

        mascotaId = intent.getIntExtra(EXTRA_MASCOTA_ID, 0)
        cuidadoId = intent.getIntExtra(EXTRA_CUIDADO_ID, 0)
        val reglaId = intent.getIntExtra(EXTRA_REGLA_ID, 0)
        if (reglaId != 0) {
            recomendacion = RecomendacionRespuesta(
                id = reglaId,
                nombre = intent.getStringExtra(EXTRA_REGLA_NOMBRE).orEmpty(),
                descripcion = intent.getStringExtra(EXTRA_REGLA_DESCRIPCION).orEmpty(),
                importancia = intent.getStringExtra(EXTRA_REGLA_IMPORTANCIA).orEmpty(),
            )
        }
        formulario = FormularioCuidado(this, binding.formulario)

        binding.tituloPantalla.setText(
            when {
                editando -> R.string.cuidado_editar_titulo
                recomendacion != null -> R.string.cuidado_configurar_titulo_simple
                else -> R.string.cuidado_personalizado_titulo
            },
        )
        if (recomendacion != null) binding.botonGuardar.setText(R.string.cuidado_confirmar)
        binding.botonVolver.setOnClickListener { finish() }
        binding.botonGuardar.setOnClickListener { guardar() }

        cargar()
    }

    private fun cargar() {
        lifecycleScope.launch {
            if (editando) {
                when (val resultado = repositorio.obtener(cuidadoId)) {
                    is Resultado.Exito -> {
                        cuidado = resultado.dato
                        mascotaId = resultado.dato.mascotaId
                        formulario.cargarEdicion(resultado.dato)
                    }
                    is Resultado.Fallo -> return@launch mostrarError(resultado.paraMostrar(this@CuidadoFormActivity))
                }
            } else {
                val elegida = recomendacion
                if (elegida != null) formulario.cargarRecomendacion(elegida) else formulario.cargarPersonalizado()
            }

            when (val resultado = mascotas.obtener(mascotaId)) {
                is Resultado.Exito -> {
                    binding.fotoMascota.mostrarMascota(resultado.dato)
                    binding.nombreMascota.text = resultado.dato.nombre
                }
                is Resultado.Fallo -> return@launch mostrarError(resultado.paraMostrar(this@CuidadoFormActivity))
            }
            binding.cuerpo.visibility = View.VISIBLE
        }
    }

    private fun guardar() {
        val configuracion = formulario.leer()
        if (configuracion == null) {
            Toast.makeText(this, R.string.revisa_campos, Toast.LENGTH_SHORT).show()
            return
        }

        binding.textoError.visibility = View.GONE
        binding.botonGuardar.isEnabled = false
        val elegida = recomendacion

        lifecycleScope.launch {
            // Cada caso devuelve el mensaje de éxito o el error, y se termina igual en todos.
            val resultado: Resultado<String> = if (elegida != null) {
                // RF25: una recomendación se agrega con su nombre y descripción de la regla.
                when (val r = repositorio.activarRecomendados(mascotaId, listOf(configuracion.paraRecomendacion(elegida.id)))) {
                    is Resultado.Exito -> Resultado.Exito(getString(R.string.recomendacion_agregada))
                    is Resultado.Fallo -> r
                }
            } else {
                // En un cuidado recomendado el nombre y la descripción están bloqueados: no se envían.
                val escribeNombre = !editando || cuidado?.esRecomendado == false
                val peticion = configuracion.paraGuardar(
                    nombre = if (escribeNombre) formulario.nombrePersonalizado else null,
                    descripcion = if (escribeNombre) formulario.descripcionPersonalizada else null,
                )
                val r = if (editando) repositorio.editar(cuidadoId, peticion) else repositorio.crearPersonalizado(mascotaId, peticion)
                when (r) {
                    is Resultado.Exito -> Resultado.Exito(r.dato.mensaje)
                    is Resultado.Fallo -> r
                }
            }

            when (resultado) {
                is Resultado.Exito -> {
                    Toast.makeText(this@CuidadoFormActivity, resultado.dato, Toast.LENGTH_SHORT).show()
                    SeleccionCuidados.mascotaId = mascotaId // la lista debe mostrar la mascota que se acaba de tocar
                    finish()
                }
                is Resultado.Fallo -> {
                    mostrarError(resultado.paraMostrar(this@CuidadoFormActivity))
                    binding.botonGuardar.isEnabled = true
                }
            }
        }
    }

    private fun mostrarError(mensaje: String) {
        binding.textoError.text = mensaje
        binding.textoError.visibility = View.VISIBLE
    }

    companion object {
        private const val EXTRA_MASCOTA_ID = "mascota_id"
        private const val EXTRA_CUIDADO_ID = "cuidado_id"
        private const val EXTRA_REGLA_ID = "regla_id"
        private const val EXTRA_REGLA_NOMBRE = "regla_nombre"
        private const val EXTRA_REGLA_DESCRIPCION = "regla_descripcion"
        private const val EXTRA_REGLA_IMPORTANCIA = "regla_importancia"

        fun intentNuevo(contexto: Context, mascotaId: Int) =
            Intent(contexto, CuidadoFormActivity::class.java).putExtra(EXTRA_MASCOTA_ID, mascotaId)

        /** RF25: configurar una recomendación para agregarla al cronograma de la mascota. */
        fun intentRecomendacion(contexto: Context, mascotaId: Int, recomendacion: RecomendacionRespuesta) =
            Intent(contexto, CuidadoFormActivity::class.java)
                .putExtra(EXTRA_MASCOTA_ID, mascotaId)
                .putExtra(EXTRA_REGLA_ID, recomendacion.id)
                .putExtra(EXTRA_REGLA_NOMBRE, recomendacion.nombre)
                .putExtra(EXTRA_REGLA_DESCRIPCION, recomendacion.descripcion)
                .putExtra(EXTRA_REGLA_IMPORTANCIA, recomendacion.importancia)

        fun intentEditar(contexto: Context, cuidadoId: Int) =
            Intent(contexto, CuidadoFormActivity::class.java).putExtra(EXTRA_CUIDADO_ID, cuidadoId)
    }
}
