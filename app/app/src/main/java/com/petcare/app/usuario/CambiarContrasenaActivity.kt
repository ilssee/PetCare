package com.petcare.app.usuario

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
import com.petcare.app.databinding.ActivityCambiarContrasenaBinding
import kotlinx.coroutines.launch

/** RF04: cambio de contraseña. Pide la actual para confirmar que es el dueño de la cuenta. */
class CambiarContrasenaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCambiarContrasenaBinding
    private val repositorio = UsuarioRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCambiarContrasenaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.raiz.respetarBarrasYTeclado()

        binding.botonVolver.setOnClickListener { finish() }
        binding.ojoActual.setOnClickListener { binding.campoActual.alternarVisibilidadContrasena(binding.ojoActual) }
        binding.ojoNueva.setOnClickListener { binding.campoNueva.alternarVisibilidadContrasena(binding.ojoNueva) }
        binding.ojoRepetir.setOnClickListener { binding.campoRepetir.alternarVisibilidadContrasena(binding.ojoRepetir) }
        binding.botonGuardarContrasena.setOnClickListener { guardar() }
    }

    private fun guardar() {
        val actual = binding.campoActual.text.toString()
        val nueva = binding.campoNueva.text.toString()
        val repetir = binding.campoRepetir.text.toString()

        binding.errorActual.visibility = View.GONE
        binding.errorNueva.visibility = View.GONE
        binding.errorRepetir.visibility = View.GONE
        binding.textoError.visibility = View.GONE

        var hayErrores = false
        if (actual.isEmpty()) {
            binding.errorActual.visibility = View.VISIBLE
            hayErrores = true
        }
        if (nueva.isEmpty()) {
            binding.errorNueva.setText(R.string.error_campo_obligatorio)
            binding.errorNueva.visibility = View.VISIBLE
            hayErrores = true
        } else if (nueva.length < 8 || nueva.length > 32) {
            binding.errorNueva.setText(R.string.error_contrasena_largo)
            binding.errorNueva.visibility = View.VISIBLE
            hayErrores = true
        }
        if (repetir.isEmpty()) {
            binding.errorRepetir.setText(R.string.error_campo_obligatorio)
            binding.errorRepetir.visibility = View.VISIBLE
            hayErrores = true
        } else if (nueva != repetir) {
            binding.errorRepetir.setText(R.string.error_contrasenas_no_coinciden)
            binding.errorRepetir.visibility = View.VISIBLE
            hayErrores = true
        }
        if (hayErrores) return

        binding.botonGuardarContrasena.isEnabled = false
        lifecycleScope.launch {
            when (val resultado = repositorio.cambiarContrasena(actual, nueva)) {
                is Resultado.Exito -> {
                    Toast.makeText(this@CambiarContrasenaActivity, R.string.contrasena_actualizada, Toast.LENGTH_SHORT).show()
                    finish()
                }
                is Resultado.Fallo -> {
                    binding.textoError.text = resultado.paraMostrar(this@CambiarContrasenaActivity)
                    binding.textoError.visibility = View.VISIBLE
                    binding.botonGuardarContrasena.isEnabled = true
                }
            }
        }
    }
}
