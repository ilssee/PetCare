package com.petcare.app.mascota

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.petcare.app.R
import com.petcare.app.common.OpcionHoja
import com.petcare.app.common.Resultado
import com.petcare.app.common.mostrarHojaOpciones
import com.petcare.app.common.paraMostrar
import com.petcare.app.cuidado.CuidadoRepository
import com.petcare.app.cuidado.CuidadoRespuesta
import com.petcare.app.cuidado.InfoPreventivaActivity
import com.petcare.app.cuidado.abrirCuidadosDe
import com.petcare.app.cuidado.mostrarCuidado
import com.petcare.app.databinding.ActivityMascotaPerfilBinding
import com.petcare.app.databinding.ItemCronogramaBinding
import kotlinx.coroutines.launch

/** Perfil de una mascota: datos, y menú para editar, inactivar/reactivar o eliminar (con confirmación). */
class MascotaPerfilActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMascotaPerfilBinding
    private val repositorio = MascotaRepository()
    private val cuidados = CuidadoRepository()
    private var mascotaId = 0
    private var mascota: MascotaRespuesta? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMascotaPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.raiz) { vista, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(0, barras.top, 0, 0)
            insets
        }

        mascotaId = intent.getIntExtra(EXTRA_ID, 0)
        binding.botonVolver.setOnClickListener { finish() }
        binding.botonOpciones.setOnClickListener { mostrarMenu() }
        binding.tarjetaInfoPreventiva.setOnClickListener {
            startActivity(InfoPreventivaActivity.intent(this, mascotaId))
        }
        // "Ver todos" lleva a la pestaña Cuidados con esta mascota elegida.
        binding.verTodos.setOnClickListener { abrirCuidadosDe(mascotaId) }
    }

    // Se recarga al volver del formulario de edición.
    override fun onResume() {
        super.onResume()
        cargar()
    }

    private fun cargar() {
        lifecycleScope.launch {
            when (val resultado = repositorio.obtener(mascotaId)) {
                is Resultado.Exito -> mostrar(resultado.dato)
                is Resultado.Fallo -> mostrarError(resultado.paraMostrar(this@MascotaPerfilActivity))
            }
        }
    }

    private fun mostrar(m: MascotaRespuesta) {
        mascota = m
        val vacio = getString(R.string.dato_vacio)
        val color = ContextCompat.getColor(this, Especies.color(m.especie))
        binding.raiz.setBackgroundColor(color)
        binding.ilustracion.mostrarMascota(m)
        binding.textoError.visibility = View.GONE

        binding.nombre.text = m.nombre
        binding.infoPreventivaSubtitulo.text = getString(R.string.info_preventiva_subtitulo, m.nombre)
        binding.estado.setText(if (m.estaInactiva) R.string.estado_inactiva else R.string.estado_activa)
        binding.estado.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this, if (m.estaInactiva) R.color.estado_inactiva_fondo else R.color.estado_activa_fondo),
        )
        binding.estado.setTextColor(
            ContextCompat.getColor(this, if (m.estaInactiva) R.color.white else R.color.estado_activa_texto),
        )

        // Raza, fecha de nacimiento y peso son opcionales: si faltan, se muestra un guion.
        binding.edad.text = m.edadTexto?.substringBefore(' ') ?: vacio
        binding.edadUnidad.text = m.edadTexto?.substringAfter(' ', "") ?: ""
        binding.peso.text = m.peso?.comoPeso() ?: vacio
        binding.sexo.text = m.sexo.conPrimeraMayuscula()

        binding.especie.text = getString(Especies.nombre(m.especie))
        binding.raza.text = m.raza ?: vacio
        binding.nacimiento.text = m.fechaNacimiento?.comoFechaLegible() ?: vacio

        // Una mascota inactiva ya no aparece en Cronograma ni en Cuidados: sus cuidados se consultan desde aquí (RF23).
        binding.verTodos.setText(if (m.estaInactiva) R.string.ver_cuidados else R.string.ver_todos)
        cargarProximosCuidados(m)
    }

    /** Los dos cuidados pendientes más próximos de la mascota, con los vencidos primero. */
    private fun cargarProximosCuidados(m: MascotaRespuesta) {
        lifecycleScope.launch {
            when (val resultado = cuidados.listar(m.id, "pendientes")) {
                is Resultado.Exito -> mostrarProximos(m, resultado.dato.cuidados.take(MAXIMO_PROXIMOS))
                is Resultado.Fallo -> mostrarError(resultado.paraMostrar(this@MascotaPerfilActivity))
            }
        }
    }

    private fun mostrarProximos(m: MascotaRespuesta, proximos: List<CuidadoRespuesta>) {
        binding.listaProximos.removeAllViews()
        binding.sinCuidados.visibility = if (proximos.isEmpty()) View.VISIBLE else View.GONE
        proximos.forEach { c ->
            val fila = ItemCronogramaBinding.inflate(layoutInflater, binding.listaProximos, false)
            fila.mostrarCuidado(c, m)
            fila.tarjeta.setOnClickListener { abrirCuidadosDe(m.id) }
            binding.listaProximos.addView(fila.root)
        }
    }

    /** Opciones de la mascota: una hoja que sube desde abajo, como en el prototipo. */
    private fun mostrarMenu() {
        val m = mascota ?: return
        mostrarHojaOpciones(
            m.nombre,
            listOf(
                OpcionHoja(R.drawable.ic_editar, R.string.mascota_menu_editar) {
                    startActivity(Intent(this, MascotaFormActivity::class.java).putExtra(MascotaFormActivity.EXTRA_ID, m.id))
                },
                OpcionHoja(
                    if (m.estaInactiva) R.drawable.ic_campana else R.drawable.ic_campana_apagada,
                    if (m.estaInactiva) R.string.mascota_menu_reactivar else R.string.mascota_menu_inactivar,
                ) { confirmarCambioDeEstado(m) },
                OpcionHoja(R.drawable.ic_eliminar, R.string.mascota_menu_eliminar, peligro = true) { confirmarEliminacion(m) },
            ),
        )
    }

    /** RF07 y RF08: pide confirmación; si se cancela, el estado no cambia. */
    private fun confirmarCambioDeEstado(m: MascotaRespuesta) {
        val inactivando = !m.estaInactiva
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(if (inactivando) R.string.mascota_inactivar_titulo else R.string.mascota_reactivar_titulo, m.nombre))
            .setMessage(if (inactivando) R.string.mascota_inactivar_mensaje else R.string.mascota_reactivar_mensaje)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(if (inactivando) R.string.mascota_menu_inactivar else R.string.mascota_menu_reactivar) { _, _ ->
                cambiarEstado(m)
            }
            .show()
    }

    private fun cambiarEstado(m: MascotaRespuesta) {
        lifecycleScope.launch {
            val resultado = if (m.estaInactiva) repositorio.reactivar(m.id) else repositorio.inactivar(m.id)
            when (resultado) {
                is Resultado.Exito -> {
                    mostrar(resultado.dato)
                    avisar(if (resultado.dato.estaInactiva) R.string.mascota_inactivada else R.string.mascota_reactivada)
                }
                is Resultado.Fallo -> mostrarError(resultado.paraMostrar(this@MascotaPerfilActivity))
            }
        }
    }

    private fun confirmarEliminacion(m: MascotaRespuesta) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.mascota_eliminar_titulo, m.nombre))
            .setMessage(R.string.mascota_eliminar_mensaje)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.mascota_menu_eliminar) { _, _ -> eliminar(m) }
            .show()
    }

    private fun eliminar(m: MascotaRespuesta) {
        lifecycleScope.launch {
            when (val resultado = repositorio.eliminar(m.id)) {
                is Resultado.Exito -> {
                    avisar(R.string.mascota_eliminada)
                    finish()
                }
                is Resultado.Fallo -> mostrarError(resultado.paraMostrar(this@MascotaPerfilActivity))
            }
        }
    }

    private fun avisar(@StringRes mensaje: Int) = Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()

    private fun mostrarError(mensaje: String) {
        binding.textoError.text = mensaje
        binding.textoError.visibility = View.VISIBLE
    }

    companion object {
        const val EXTRA_ID = "mascota_id"
        private const val MAXIMO_PROXIMOS = 2
    }
}
