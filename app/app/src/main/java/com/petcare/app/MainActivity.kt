package com.petcare.app

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.petcare.app.common.SessionManager
import com.petcare.app.common.UbicacionActual
import com.petcare.app.databinding.ActivityMainBinding
import com.petcare.app.cronograma.CronogramaFragment
import com.petcare.app.cuidado.CuidadosFragment
import com.petcare.app.mapa.MapaFragment
import com.petcare.app.mascota.MascotasFragment
import com.petcare.app.notificacion.Notificaciones
import com.petcare.app.usuario.LoginActivity
import com.petcare.app.usuario.PerfilFragment
import kotlinx.coroutines.launch

/** Pantalla principal: contiene la barra de navegación inferior y muestra una sección a la vez. */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Android 13+ pide permiso para mostrar notificaciones. Se acepte o no, se registra el celular.
    // Después se pide la ubicación (uno a la vez, para no mostrar dos cuadros juntos).
    private val permisoNotificaciones =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            registrarDispositivo()
            pedirUbicacion()
        }

    // El cuadro de Android de la ubicación. El usuario puede dar solo la aproximada o no darla.
    private val permisoUbicacion =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

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
                    R.id.nav_mapa -> MapaFragment()
                    R.id.nav_perfil -> PerfilFragment()
                    else -> MascotasFragment()
                },
            )
            true
        }

        if (savedInstanceState == null) {
            binding.navegacion.selectedItemId = seccionPedida(intent)
            activarNotificaciones()
        }
    }

    // Al abrir la app se piden los permisos que falten. Android deja de mostrar el cuadro si el usuario lo
    // rechaza un par de veces; entonces solo se pueden activar desde los ajustes del celular.
    private fun activarNotificaciones() {
        if (Notificaciones.tienePermiso(this)) {
            registrarDispositivo()
            pedirUbicacion()
        } else {
            permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun pedirUbicacion() {
        if (!UbicacionActual.tienePermiso(this)) permisoUbicacion.launch(UbicacionActual.PERMISOS)
    }

    private fun registrarDispositivo() {
        lifecycleScope.launch { Notificaciones.registrarDispositivo() }
    }

    // Otra pantalla puede pedir que se abra una sección concreta (por ejemplo Cuidados tras activar el cronograma).
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (::binding.isInitialized && intent.hasExtra(EXTRA_SECCION)) {
            binding.navegacion.selectedItemId = seccionPedida(intent)
        }
    }

    /**
     * Sección que debe abrirse. Las pantallas de la app envían el id de la opción (un número). Los avisos
     * push que se tocan con la app cerrada traen el nombre de la sección como texto ("mapa" o "cuidados").
     */
    private fun seccionPedida(intent: Intent): Int {
        val id = intent.getIntExtra(EXTRA_SECCION, 0)
        if (id != 0) return id
        return when (intent.getStringExtra(EXTRA_SECCION)) {
            "mapa" -> R.id.nav_mapa
            "cuidados" -> R.id.nav_cuidados
            else -> R.id.nav_cronograma
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
