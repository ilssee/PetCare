package com.petcare.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.petcare.app.common.PlaceholderFragment
import com.petcare.app.common.SessionManager
import com.petcare.app.databinding.ActivityMainBinding
import com.petcare.app.cronograma.CronogramaFragment
import com.petcare.app.cuidado.CuidadosFragment
import com.petcare.app.mascota.MascotasFragment
import com.petcare.app.usuario.LoginActivity
import com.petcare.app.usuario.PerfilFragment

/** Pantalla principal: contiene la barra de navegación inferior y muestra una sección a la vez. */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!SessionManager.haySesion) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.raiz) { vista, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(barras.left, 0, barras.right, 0)
            insets
        }

        binding.navegacion.setOnItemSelectedListener { opcion ->
            mostrar(
                when (opcion.itemId) {
                    R.id.nav_cronograma -> CronogramaFragment()
                    R.id.nav_cuidados -> CuidadosFragment()
                    R.id.nav_mapa -> PlaceholderFragment.nuevo(getString(R.string.nav_mapa))
                    R.id.nav_perfil -> PerfilFragment()
                    else -> MascotasFragment()
                },
            )
            true
        }

        if (savedInstanceState == null) {
            binding.navegacion.selectedItemId = intent.getIntExtra(EXTRA_SECCION, R.id.nav_mascotas)
        }
    }

    // Otra pantalla puede pedir que se abra una sección concreta (por ejemplo Cuidados tras activar el cronograma).
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (::binding.isInitialized && intent.hasExtra(EXTRA_SECCION)) {
            binding.navegacion.selectedItemId = intent.getIntExtra(EXTRA_SECCION, R.id.nav_mascotas)
        }
    }

    private fun mostrar(fragmento: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.contenedor, fragmento).commit()
    }

    companion object {
        /** Id de la opción de la barra inferior que debe quedar seleccionada (por ejemplo R.id.nav_cuidados). */
        const val EXTRA_SECCION = "seccion"
    }
}
