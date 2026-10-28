package com.petcare.app.cuidado

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.petcare.app.R
import com.petcare.app.common.Resultado
import com.petcare.app.common.paraMostrar
import com.petcare.app.databinding.ActivityCuidadoFormBinding
import com.petcare.app.mascota.MascotaRepository
import com.petcare.app.mascota.mostrarMascota
import kotlinx.coroutines.launch

/**
 * Agregar un cuidado personalizado (RF12) o editar uno existente (RF14).
 * Si recibe EXTRA_CUIDADO_ID edita ese cuidado; si no, agrega uno nuevo a la mascota de EXTRA_MASCOTA_ID.
 */
class CuidadoFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCuidadoFormBinding
    private lateinit var formulario: FormularioCuidado
    private val mascotas = MascotaRepository()
    private val repositorio = CuidadoRepository()

    private var mascotaId = 0
    private var cuidadoId = 0
    private var cuidado: CuidadoRespuesta? = null

    private val editando get() = cuidadoId != 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCuidadoFormBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.raiz) { vista, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(0, barras.top, 0, barras.bottom)
            insets
        }

        mascotaId = intent.getIntExtra(EXTRA_MASCOTA_ID, 0)
        cuidadoId = intent.getIntExtra(EXTRA_CUIDADO_ID, 0)
        formulario = FormularioCuidado(this, binding.formulario)

        binding.tituloPantalla.setText(if (editando) R.string.cuidado_editar_titulo else R.string.cuidado_personalizado_titulo)
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
                formulario.cargarPersonalizado()
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

        // En un cuidado recomendado el nombre y la descripción están bloqueados: no se envían.
        val escribeNombre = !editando || cuidado?.esRecomendado == false
        val peticion = configuracion.paraGuardar(
            nombre = if (escribeNombre) formulario.nombrePersonalizado else null,
            descripcion = if (escribeNombre) formulario.descripcionPersonalizada else null,
        )

        binding.textoError.visibility = View.GONE
        binding.botonGuardar.isEnabled = false
        lifecycleScope.launch {
            val resultado = if (editando) repositorio.editar(cuidadoId, peticion)
            else repositorio.crearPersonalizado(mascotaId, peticion)
            when (resultado) {
                is Resultado.Exito -> {
                    Toast.makeText(this@CuidadoFormActivity, resultado.dato.mensaje, Toast.LENGTH_SHORT).show()
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

        fun intentNuevo(contexto: Context, mascotaId: Int) =
            Intent(contexto, CuidadoFormActivity::class.java).putExtra(EXTRA_MASCOTA_ID, mascotaId)

        fun intentEditar(contexto: Context, cuidadoId: Int) =
            Intent(contexto, CuidadoFormActivity::class.java).putExtra(EXTRA_CUIDADO_ID, cuidadoId)
    }
}
