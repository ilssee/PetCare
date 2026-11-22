package com.petcare.app.usuario

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.petcare.app.R
import com.petcare.app.common.respetarBarrasYTeclado
import com.petcare.app.common.confirmarAlRetroceder
import com.petcare.app.common.salirConfirmando
import com.petcare.app.common.Resultado
import com.petcare.app.common.SessionManager
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

        binding.botonVolver.setOnClickListener { salirConfirmando(::hayCambiosSinGuardar) }
        confirmarAlRetroceder(::hayCambiosSinGuardar)
        binding.ojoActual.setOnClickListener { binding.campoActual.alternarVisibilidadContrasena(binding.ojoActual) }
        binding.ojoNueva.setOnClickListener { binding.campoNueva.alternarVisibilidadContrasena(binding.ojoNueva) }
        binding.ojoRepetir.setOnClickListener { binding.campoRepetir.alternarVisibilidadContrasena(binding.ojoRepetir) }
        binding.botonGuardarContrasena.setOnClickListener { guardar() }
    }

    /** Hay cambios si alguno de los tres campos tiene texto. */
    private fun hayCambiosSinGuardar(): Boolean =
        listOf(binding.campoActual, binding.campoNueva, binding.campoRepetir)
            .any { it.text.toString().isNotEmpty() }

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
        } else if (nueva.length < 8 || nueva.length > 64) {
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
                    // ERS: al cambiar la contraseña se cierra la sesión; hay que ingresar de nuevo.
                    SessionManager.cerrar()
                    startActivity(
                        Intent(this@CambiarContrasenaActivity, LoginActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                    )
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
