package com.petcare.app.cuidado

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.petcare.app.MainActivity
import com.petcare.app.R
import com.petcare.app.common.Resultado
import com.petcare.app.common.configurar
import com.petcare.app.common.paraMostrar
import com.petcare.app.databinding.ActivityPropuestaBinding
import com.petcare.app.databinding.ItemRecomendacionBinding
import com.petcare.app.mascota.Especies
import com.petcare.app.mascota.MascotaRepository
import com.petcare.app.mascota.MascotaRespuesta
import com.petcare.app.mascota.conPrimeraMayuscula
import com.petcare.app.mascota.mostrarMascota
import kotlinx.coroutines.launch

/**
 * Propuesta de cronograma de una mascota (RF09, RF10 y RF11).
 *  1. Muestra los cuidados recomendados con una casilla cada uno; el usuario desmarca los que no quiere.
 *  2. Al confirmar, pide configurar uno por uno (modo de término, frecuencia, fecha de inicio,
 *     aviso anticipado y nota), con opción de omitir alguno.
 *  3. Envía los cuidados configurados y lleva a la sección Cuidados.
 */
class PropuestaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPropuestaBinding
    private lateinit var formulario: FormularioCuidado
    private val mascotas = MascotaRepository()
    private val repositorio = CuidadoRepository()

    private var mascotaId = 0
    private var recienRegistrada = false
    private var mascota: MascotaRespuesta? = null
    private var recomendaciones: List<RecomendacionRespuesta> = emptyList()

    // Estado de la lista: qué tarjetas están desmarcadas y cuáles abiertas (por id de recomendación).
    private val desmarcadas = mutableSetOf<Int>()
    private val expandidas = mutableSetOf<Int>()
    private val tarjetas = mutableMapOf<Int, ItemRecomendacionBinding>()

    // Paso 2: los cuidados elegidos, cuál se está configurando y los ya configurados.
    private var configurando = false
    private var seleccion: List<RecomendacionRespuesta> = emptyList()
    private var paso = 0
    private val configurados = mutableListOf<CuidadoElegido>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPropuestaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.raiz) { vista, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(0, barras.top, 0, barras.bottom)
            insets
        }

        mascotaId = intent.getIntExtra(EXTRA_MASCOTA_ID, 0)
        recienRegistrada = intent.getBooleanExtra(EXTRA_RECIEN_REGISTRADA, false)
        formulario = FormularioCuidado(this, binding.formulario)

        // Atrás en el paso 2 vuelve a la lista (se descartan las configuraciones); en la lista, sale.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (configurando) volverALaLista() else finish()
            }
        })
        binding.botonVolver.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.botonPrincipal.setOnClickListener { if (configurando) confirmarPaso() else comenzarConfiguracion() }
        binding.botonSecundario.setOnClickListener { if (configurando) avanzar() else finish() }
        binding.seleccionarTodos.setOnClickListener { alternarSeleccionDeTodos() }
        binding.expandirTodo.setOnClickListener { alternarExpansionDeTodos() }

        cargar()
    }

    private fun cargar() {
        lifecycleScope.launch {
            val resultadoMascota = mascotas.obtener(mascotaId)
            val resultadoRecomendaciones = repositorio.recomendaciones(mascotaId)
            if (resultadoMascota is Resultado.Exito && resultadoRecomendaciones is Resultado.Exito) {
                mascota = resultadoMascota.dato
                recomendaciones = resultadoRecomendaciones.dato
                mostrarLista()
            } else {
                // RF09: si falla la consulta de cuidados, la mascota ya quedó guardada.
                val fallo = (resultadoMascota as? Resultado.Fallo) ?: (resultadoRecomendaciones as Resultado.Fallo)
                mostrarError(
                    if (recienRegistrada) getString(R.string.propuesta_error_generar)
                    else fallo.paraMostrar(this@PropuestaActivity),
                )
            }
        }
    }

    // ---------- Paso 1: elegir cuidados ----------

    private fun mostrarLista() {
        configurando = false
        binding.tituloPantalla.setText(R.string.propuesta_titulo)
        binding.seccionFormulario.visibility = View.GONE
        binding.seccionLista.visibility = View.VISIBLE
        binding.textoError.visibility = View.GONE

        val m = mascota ?: return
        binding.fotoMascota.mostrarMascota(m)
        binding.nombreMascota.text = getString(R.string.propuesta_para, m.nombre)
        binding.detalleMascota.text = getString(
            R.string.propuesta_especie_sexo,
            getString(Especies.nombre(m.especie)),
            m.sexo.conPrimeraMayuscula(),
        )

        dibujarTarjetas()
        actualizarControles()
    }

    private fun dibujarTarjetas() {
        val contenedor = binding.contenedorRecomendaciones
        contenedor.removeAllViews()
        tarjetas.clear()

        if (recomendaciones.isEmpty()) {
            binding.filaControles.visibility = View.GONE
            binding.vacio.configurar(
                R.drawable.ic_corazon,
                R.string.propuesta_todo_agregado_titulo,
                R.string.propuesta_todo_agregado_mensaje,
            ) {}
            binding.vacio.boton.visibility = View.GONE
            binding.vacio.root.visibility = View.VISIBLE
            return
        }

        binding.filaControles.visibility = View.VISIBLE
        binding.vacio.root.visibility = View.GONE
        recomendaciones.forEach { recomendacion ->
            val tarjeta = ItemRecomendacionBinding.inflate(layoutInflater, contenedor, false)
            tarjeta.nombre.text = recomendacion.nombre
            tarjeta.descripcion.text = recomendacion.descripcion
            tarjeta.importancia.text = recomendacion.importancia
            tarjeta.casilla.setOnClickListener {
                if (!desmarcadas.remove(recomendacion.id)) desmarcadas.add(recomendacion.id)
                actualizarTarjeta(recomendacion.id)
                actualizarControles()
            }
            tarjeta.cabecera.setOnClickListener {
                if (!expandidas.remove(recomendacion.id)) expandidas.add(recomendacion.id)
                actualizarTarjeta(recomendacion.id)
                actualizarControles()
            }
            tarjetas[recomendacion.id] = tarjeta
            actualizarTarjeta(recomendacion.id)
            contenedor.addView(tarjeta.root)
        }
    }

    /** Refleja el estado de una recomendación: marcada o no (atenuada) y abierta o cerrada. */
    private fun actualizarTarjeta(id: Int) {
        val tarjeta = tarjetas[id] ?: return
        val marcada = id !in desmarcadas
        val abierta = id in expandidas
        tarjeta.casilla.isSelected = marcada
        tarjeta.root.alpha = if (marcada) 1f else 0.55f
        tarjeta.detalle.visibility = if (abierta) View.VISIBLE else View.GONE
        tarjeta.flecha.rotation = if (abierta) 180f else 0f
    }

    private fun alternarSeleccionDeTodos() {
        if (desmarcadas.isEmpty()) desmarcadas.addAll(recomendaciones.map { it.id }) else desmarcadas.clear()
        recomendaciones.forEach { actualizarTarjeta(it.id) }
        actualizarControles()
    }

    private fun alternarExpansionDeTodos() {
        if (expandidas.size == recomendaciones.size) expandidas.clear() else expandidas.addAll(recomendaciones.map { it.id })
        recomendaciones.forEach { actualizarTarjeta(it.id) }
        actualizarControles()
    }

    /** "Seleccionar todos (n/total)", "Expandir/Contraer todo" y el botón de confirmar. */
    private fun actualizarControles() {
        if (recomendaciones.isEmpty()) {
            binding.pie.visibility = View.GONE
            return
        }
        val total = recomendaciones.size
        val elegidas = recomendaciones.count { it.id !in desmarcadas }

        binding.casillaTodos.isSelected = elegidas == total
        binding.textoSeleccionarTodos.text = getString(R.string.propuesta_seleccionar_todos, elegidas, total)
        binding.expandirTodo.setText(
            if (expandidas.size == total) R.string.propuesta_contraer_todo else R.string.propuesta_expandir_todo,
        )

        binding.pie.visibility = View.VISIBLE
        binding.botonPrincipal.isEnabled = elegidas > 0
        binding.botonPrincipal.alpha = if (elegidas > 0) 1f else 0.5f
        binding.botonPrincipal.text =
            if (elegidas > 0) getString(R.string.propuesta_confirmar, elegidas)
            else getString(R.string.propuesta_seleccionar_uno)
        binding.botonSecundario.setText(R.string.propuesta_ahora_no)
        binding.botonSecundario.isEnabled = true
        binding.botonSecundario.visibility = View.VISIBLE
    }

    // ---------- Paso 2: configurar cada cuidado ----------

    private fun comenzarConfiguracion() {
        seleccion = recomendaciones.filter { it.id !in desmarcadas }
        if (seleccion.isEmpty()) return
        paso = 0
        configurados.clear()
        configurando = true
        mostrarPaso()
    }

    private fun mostrarPaso() {
        val recomendacion = seleccion[paso]
        val m = mascota
        binding.tituloPantalla.text = getString(R.string.cuidado_configurar_titulo, paso + 1, seleccion.size)
        if (m != null) {
            binding.fotoMascotaFormulario.mostrarMascota(m)
            binding.paraMascota.text = m.nombre
        }
        formulario.cargarRecomendacion(recomendacion)

        binding.seccionLista.visibility = View.GONE
        binding.seccionFormulario.visibility = View.VISIBLE
        binding.seccionFormulario.scrollTo(0, 0)
        binding.textoError.visibility = View.GONE

        binding.pie.visibility = View.VISIBLE
        binding.botonPrincipal.isEnabled = true
        binding.botonPrincipal.alpha = 1f
        binding.botonPrincipal.setText(R.string.cuidado_confirmar)
        binding.botonSecundario.setText(R.string.cuidado_omitir)
        binding.botonSecundario.isEnabled = true
        binding.botonSecundario.visibility = View.VISIBLE
    }

    private fun confirmarPaso() {
        val configuracion = formulario.leer()
        if (configuracion == null) {
            Toast.makeText(this, R.string.revisa_campos, Toast.LENGTH_SHORT).show()
            return
        }
        configurados.add(configuracion.paraRecomendacion(seleccion[paso].id))
        avanzar()
    }

    /** Pasa al siguiente cuidado (también al omitir uno). Tras el último, activa el cronograma. */
    private fun avanzar() {
        paso++
        if (paso < seleccion.size) mostrarPaso() else activar()
    }

    private fun volverALaLista() {
        configurados.clear()
        mostrarLista()
    }

    // ---------- Confirmar ----------

    private fun activar() {
        if (configurados.isEmpty()) {
            // Se omitieron todos: no hay nada que activar.
            Toast.makeText(this, R.string.propuesta_ninguno_configurado, Toast.LENGTH_LONG).show()
            volverALaLista()
            return
        }

        binding.botonPrincipal.isEnabled = false
        binding.botonSecundario.isEnabled = false
        lifecycleScope.launch {
            when (val resultado = repositorio.activarRecomendados(mascotaId, configurados)) {
                is Resultado.Exito -> {
                    Toast.makeText(this@PropuestaActivity, R.string.cronograma_activado, Toast.LENGTH_SHORT).show()
                    irACuidados()
                }
                is Resultado.Fallo -> {
                    binding.botonPrincipal.isEnabled = true
                    binding.botonSecundario.isEnabled = true
                    MaterialAlertDialogBuilder(this@PropuestaActivity)
                        .setMessage(resultado.paraMostrar(this@PropuestaActivity))
                        .setCancelable(false)
                        .setNegativeButton(R.string.cancelar) { _, _ -> volverALaLista() }
                        .setPositiveButton(R.string.reintentar) { _, _ -> activar() }
                        .show()
                }
            }
        }
    }

    /** RF11: tras activar el cronograma se redirige a la pantalla de cuidados. */
    private fun irACuidados() {
        SeleccionCuidados.mascotaId = mascotaId // la lista debe mostrar la mascota que se acaba de configurar
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(MainActivity.EXTRA_SECCION, R.id.nav_cuidados),
        )
        finish()
    }

    private fun mostrarError(mensaje: String) {
        binding.textoError.text = mensaje
        binding.textoError.visibility = View.VISIBLE
    }

    companion object {
        private const val EXTRA_MASCOTA_ID = "mascota_id"
        private const val EXTRA_RECIEN_REGISTRADA = "recien_registrada"

        fun intent(contexto: Context, mascotaId: Int, recienRegistrada: Boolean = false) =
            Intent(contexto, PropuestaActivity::class.java)
                .putExtra(EXTRA_MASCOTA_ID, mascotaId)
                .putExtra(EXTRA_RECIEN_REGISTRADA, recienRegistrada)
    }
}
