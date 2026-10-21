package com.petcare.app.usuario

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.petcare.app.R
import com.petcare.app.common.Resultado
import com.petcare.app.common.paraMostrar
import com.petcare.app.databinding.ActivityRegistroBinding
import kotlinx.coroutines.launch

/** Pantalla de registro. Al terminar, vuelve al login (RF01) con el correo ya escrito. */
class RegistroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroBinding
    private val repositorio = UsuarioRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.raiz) { vista, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(0, barras.top, 0, barras.bottom)
            insets
        }

        binding.botonVolver.setOnClickListener { finish() }
        binding.enlaceLogin.setOnClickListener { finish() }
        binding.botonOjo.setOnClickListener { binding.campoContrasena.alternarVisibilidadContrasena() }
        binding.botonRegistrarse.setOnClickListener { registrar() }
    }

    private fun registrar() {
        val nombre = binding.campoNombre.text.toString()
        val correo = binding.campoCorreo.text.toString()
        val contrasena = binding.campoContrasena.text.toString()

        if (nombre.isBlank() || correo.isBlank() || contrasena.isEmpty()) {
            mostrarError(getString(R.string.error_todos_obligatorios))
            return
        }

        binding.botonRegistrarse.isEnabled = false
        binding.textoError.visibility = View.GONE

        lifecycleScope.launch {
            when (val resultado = repositorio.registrar(nombre, correo, contrasena)) {
                is Resultado.Exito -> {
                    setResult(RESULT_OK, Intent().putExtra(EXTRA_CORREO, correo.trim()))
                    finish()
                }
                is Resultado.Fallo -> {
                    mostrarError(resultado.paraMostrar(this@RegistroActivity))
                    binding.botonRegistrarse.isEnabled = true
                }
            }
        }
    }

    private fun mostrarError(mensaje: String) {
        binding.textoError.text = mensaje
        binding.textoError.visibility = View.VISIBLE
    }

    companion object {
        const val EXTRA_CORREO = "correo"
    }
}
