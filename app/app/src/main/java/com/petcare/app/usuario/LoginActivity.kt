package com.petcare.app.usuario

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.petcare.app.MainActivity
import com.petcare.app.R
import com.petcare.app.common.Resultado
import com.petcare.app.common.SessionManager
import com.petcare.app.common.paraMostrar
import com.petcare.app.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

/** Pantalla de inicio de sesión. */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val repositorio = UsuarioRepository()

    // Al volver del registro, deja el correo escrito y avisa que ya puede iniciar sesión.
    private val pantallaRegistro = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        val correo = resultado.data?.getStringExtra(RegistroActivity.EXTRA_CORREO)
        if (resultado.resultCode == RESULT_OK && correo != null) {
            binding.campoCorreo.setText(correo)
            binding.campoContrasena.setText("")
            binding.campoContrasena.requestFocus()
            Toast.makeText(this, R.string.cuenta_creada, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Si ya hay una sesión guardada, se salta el login.
        if (SessionManager.haySesion) {
            irAInicio()
            return
        }

        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.raiz) { vista, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(0, barras.top, 0, barras.bottom)
            insets
        }

        binding.botonOjo.setOnClickListener { binding.campoContrasena.alternarVisibilidadContrasena() }
        binding.botonIngresar.setOnClickListener { iniciarSesion() }
        binding.enlaceRegistro.setOnClickListener {
            pantallaRegistro.launch(Intent(this, RegistroActivity::class.java))
        }
    }

    private fun iniciarSesion() {
        val correo = binding.campoCorreo.text.toString()
        val contrasena = binding.campoContrasena.text.toString()

        if (correo.isBlank() || contrasena.isEmpty()) {
            mostrarError(getString(R.string.error_todos_obligatorios))
            return
        }

        binding.botonIngresar.isEnabled = false
        binding.textoError.visibility = View.GONE

        lifecycleScope.launch {
            when (val resultado = repositorio.iniciarSesion(correo, contrasena)) {
                is Resultado.Exito -> irAInicio()
                is Resultado.Fallo -> {
                    mostrarError(resultado.paraMostrar(this@LoginActivity))
                    binding.botonIngresar.isEnabled = true
                }
            }
        }
    }

    private fun mostrarError(mensaje: String) {
        binding.textoError.text = mensaje
        binding.textoError.visibility = View.VISIBLE
    }

    private fun irAInicio() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
