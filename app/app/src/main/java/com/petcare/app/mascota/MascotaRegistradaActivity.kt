package com.petcare.app.mascota

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.petcare.app.R
import com.petcare.app.common.Resultado
import com.petcare.app.cuidado.PropuestaActivity
import com.petcare.app.databinding.ActivityMascotaRegistradaBinding
import kotlinx.coroutines.launch

/**
 * Pantalla que aparece justo después de registrar una mascota: da la bienvenida y ofrece ver la
 * propuesta de cronograma (RF05 y RF09). "Ahora no" vuelve a la lista de mascotas.
 */
class MascotaRegistradaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMascotaRegistradaBinding
    private val repositorio = MascotaRepository()
    private var mascotaId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMascotaRegistradaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.raiz) { vista, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(0, barras.top, 0, barras.bottom)
            insets
        }

        mascotaId = intent.getIntExtra(EXTRA_MASCOTA_ID, 0)
        binding.raiz.visibility = View.INVISIBLE // se muestra cuando llegan los datos de la mascota
        binding.botonVerPropuesta.setOnClickListener { verPropuesta() }
        binding.botonAhoraNo.setOnClickListener { finish() }

        cargar()
    }

    private fun cargar() {
        lifecycleScope.launch {
            when (val resultado = repositorio.obtener(mascotaId)) {
                is Resultado.Exito -> mostrar(resultado.dato)
                // Sin los datos no se puede dar la bienvenida: se pasa directo a la propuesta,
                // que explica el problema si tampoco logra cargar.
                is Resultado.Fallo -> verPropuesta()
            }
        }
    }

    private fun mostrar(m: MascotaRespuesta) {
        binding.circulo.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, Especies.color(m.especie)))
        binding.ilustracion.mostrarMascota(m)
        binding.titulo.text = getString(
            if (m.sexo == "hembra") R.string.registrado_titulo_hembra else R.string.registrado_titulo_macho,
            m.nombre,
        )
        binding.mensaje.text = getString(R.string.registrado_mensaje, m.nombre)
        binding.raiz.visibility = View.VISIBLE
    }

    private fun verPropuesta() {
        startActivity(PropuestaActivity.intent(this, mascotaId, recienRegistrada = true))
        finish()
    }

    companion object {
        private const val EXTRA_MASCOTA_ID = "mascota_id"

        fun intent(contexto: Context, mascotaId: Int) =
            Intent(contexto, MascotaRegistradaActivity::class.java).putExtra(EXTRA_MASCOTA_ID, mascotaId)
    }
}
