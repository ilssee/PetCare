package com.petcare.app.usuario

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
import com.petcare.app.databinding.ActivityEditarPerfilBinding
import kotlinx.coroutines.launch

/** RF04: el usuario edita su nombre y su correo. */
class EditarPerfilActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditarPerfilBinding
    private val repositorio = UsuarioRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditarPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.raiz) { vista, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(0, barras.top, 0, barras.bottom)
            insets
        }

        binding.botonVolver.setOnClickListener { finish() }
        binding.filaCambiarContrasena.setOnClickListener {
            startActivity(Intent(this, CambiarContrasenaActivity::class.java))
        }
        binding.botonGuardar.setOnClickListener { guardar() }
        cargarDatos()
    }

    private fun cargarDatos() {
        lifecycleScope.launch {
            when (val resultado = repositorio.obtenerPerfil()) {
                is Resultado.Exito -> {
                    binding.campoNombre.setText(resultado.dato.nombre)
                    binding.campoCorreo.setText(resultado.dato.correo)
                }
                is Resultado.Fallo -> mostrarError(resultado.paraMostrar(this@EditarPerfilActivity))
            }
        }
    }

    private fun guardar() {
        val nombre = binding.campoNombre.text.toString().trim()
        val correo = binding.campoCorreo.text.toString().trim()

        // Cada campo vacío muestra "Este campo es obligatorio" (RF04).
        binding.errorNombre.visibility = if (nombre.isEmpty()) View.VISIBLE else View.GONE
        binding.errorCorreo.visibility = if (correo.isEmpty()) View.VISIBLE else View.GONE
        binding.textoError.visibility = View.GONE
        if (nombre.isEmpty() || correo.isEmpty()) return

        binding.botonGuardar.isEnabled = false
        lifecycleScope.launch {
            when (val resultado = repositorio.actualizarPerfil(nombre, correo)) {
                is Resultado.Exito -> {
                    Toast.makeText(this@EditarPerfilActivity, R.string.perfil_actualizado, Toast.LENGTH_SHORT).show()
                    finish()
                }
                is Resultado.Fallo -> {
                    mostrarError(resultado.paraMostrar(this@EditarPerfilActivity))
                    binding.botonGuardar.isEnabled = true
                }
            }
        }
    }

    private fun mostrarError(mensaje: String) {
        binding.textoError.text = mensaje
        binding.textoError.visibility = View.VISIBLE
    }
}
