package com.petcare.app.usuario

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.petcare.app.R
import com.petcare.app.common.confirmarAlRetroceder
import com.petcare.app.common.salirConfirmando
import com.petcare.app.common.respetarBarrasYTeclado
import com.petcare.app.common.Resultado
import com.petcare.app.common.paraMostrar
import com.petcare.app.databinding.ActivityEditarPerfilBinding
import kotlinx.coroutines.launch

/** RF04: el usuario edita su nombre y su correo. */
class EditarPerfilActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditarPerfilBinding
    private val repositorio = UsuarioRepository()

    // Datos tal como se cargaron, para detectar si el usuario cambió algo antes de salir.
    private var nombreOriginal: String? = null
    private var correoOriginal: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditarPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.raiz.respetarBarrasYTeclado()

        binding.botonVolver.setOnClickListener { salirConfirmando(::hayCambiosSinGuardar) }
        confirmarAlRetroceder(::hayCambiosSinGuardar)
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
                    nombreOriginal = resultado.dato.nombre.trim()
                    correoOriginal = resultado.dato.correo.trim()
                }
                is Resultado.Fallo -> mostrarError(resultado.paraMostrar(this@EditarPerfilActivity))
            }
        }
    }

    /** Hay cambios si lo escrito difiere de los datos cargados. Sin datos cargados no hay nada que perder. */
    private fun hayCambiosSinGuardar(): Boolean {
        val nombre = nombreOriginal ?: return false
        val correo = correoOriginal ?: return false
        return binding.campoNombre.text.toString().trim() != nombre ||
            binding.campoCorreo.text.toString().trim() != correo
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
