package com.petcare.app.cuidado

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.petcare.app.R
import com.petcare.app.common.Resultado
import com.petcare.app.databinding.ActivityInfoPreventivaBinding
import com.petcare.app.databinding.ItemInfoPreventivaBinding
import com.petcare.app.databinding.ItemSelectorMascotaBinding
import com.petcare.app.mascota.Especies
import com.petcare.app.mascota.MascotaRepository
import com.petcare.app.mascota.MascotaRespuesta
import com.petcare.app.mascota.mostrarMascota
import kotlinx.coroutines.launch

/**
 * Información preventiva (RF24): las recomendaciones de cuidados de la mascota elegida, que se
 * despliegan para leer su descripción e importancia. Desde aquí se puede agregar una recomendación
 * al cronograma (RF25). Las que ya están en el cronograma se marcan.
 */
class InfoPreventivaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInfoPreventivaBinding
    private val mascotas = MascotaRepository()
    private val repositorio = CuidadoRepository()

    private var todas: List<MascotaRespuesta> = emptyList()
    private var mascotaId = 0
    private var recomendaciones: List<RecomendacionRespuesta> = emptyList()
    private val expandidas = mutableSetOf<Int>() // ids de las recomendaciones desplegadas

    private val mascota get() = todas.firstOrNull { it.id == mascotaId }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityInfoPreventivaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.raiz) { vista, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(0, barras.top, 0, barras.bottom)
            insets
        }

        mascotaId = intent.getIntExtra(EXTRA_MASCOTA_ID, 0)
        binding.botonVolver.setOnClickListener { finish() }
        binding.expandirTodo.setOnClickListener {
            if (expandidas.size == recomendaciones.size) expandidas.clear() else expandidas.addAll(recomendaciones.map { it.id })
            dibujarLista()
        }
    }

    // Se vuelve a cargar al regresar de agregar una recomendación, para marcarla "en tu cronograma".
    override fun onResume() {
        super.onResume()
        cargarMascotas()
    }

    private fun cargarMascotas() {
        lifecycleScope.launch {
            when (val resultado = mascotas.listar()) {
                is Resultado.Exito -> {
                    todas = resultado.dato
                    if (mascota == null) mascotaId = todas.firstOrNull { !it.estaInactiva }?.id ?: todas.firstOrNull()?.id ?: 0
                    if (mascotaId == 0) {
                        finish()
                        return@launch
                    }
                    dibujarSelector()
                    cargarRecomendaciones()
                }
                is Resultado.Fallo -> mostrarError()
            }
        }
    }

    private fun dibujarSelector() {
        val contenedor = binding.selectorMascotas
        contenedor.removeAllViews()
        todas.forEach { m ->
            val item = ItemSelectorMascotaBinding.inflate(layoutInflater, contenedor, false)
            val elegida = m.id == mascotaId
            item.foto.mostrarMascota(m)
            item.foto.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, Especies.color(m.especie)))
            item.anillo.isSelected = elegida
            item.nombre.text = m.nombre
            item.nombre.setTextColor(ContextCompat.getColor(this, if (elegida) R.color.petcare_text else R.color.petcare_text_muted))
            item.nombre.typeface = ResourcesCompat.getFont(this, if (elegida) R.font.poppins_semibold else R.font.poppins_regular)
            item.root.setOnClickListener {
                if (m.id != mascotaId) {
                    mascotaId = m.id
                    expandidas.clear()
                    dibujarSelector()
                    cargarRecomendaciones()
                }
            }
            contenedor.addView(item.root)
        }
    }

    private fun cargarRecomendaciones() {
        val id = mascotaId
        lifecycleScope.launch {
            val resultado = repositorio.informacionPreventiva(id)
            if (id != mascotaId) return@launch // se eligió otra mascota mientras cargaba
            when (resultado) {
                is Resultado.Exito -> {
                    recomendaciones = resultado.dato
                    binding.textoError.visibility = View.GONE
                    binding.contenido.visibility = View.VISIBLE
                    dibujarLista()
                }
                is Resultado.Fallo -> mostrarError()
            }
        }
    }

    private fun dibujarLista() {
        val m = mascota ?: return
        binding.textoAviso.text = HtmlCompat.fromHtml(getString(R.string.info_aviso, m.nombre), HtmlCompat.FROM_HTML_MODE_COMPACT)
        binding.expandirTodo.setText(
            if (expandidas.size == recomendaciones.size) R.string.propuesta_contraer_todo else R.string.propuesta_expandir_todo,
        )

        binding.lista.removeAllViews()
        recomendaciones.forEach { binding.lista.addView(crearTarjeta(it, m)) }
    }

    private fun crearTarjeta(r: RecomendacionRespuesta, m: MascotaRespuesta): View {
        val t = ItemInfoPreventivaBinding.inflate(layoutInflater, binding.lista, false)
        t.nombre.text = r.nombre
        t.descripcion.text = r.descripcion
        t.importancia.text = r.importancia
        t.etiquetaCronograma.visibility = if (r.enCronograma) View.VISIBLE else View.GONE
        // Solo se puede agregar lo que aún no está en el cronograma, y a una mascota activa.
        t.botonAgregar.visibility = if (r.enCronograma || m.estaInactiva) View.GONE else View.VISIBLE
        t.botonAgregar.setOnClickListener {
            startActivity(CuidadoFormActivity.intentRecomendacion(this, m.id, r))
        }

        aplicarEstado(t, r.id)
        t.cabecera.setOnClickListener {
            if (!expandidas.remove(r.id)) expandidas.add(r.id)
            aplicarEstado(t, r.id)
            binding.expandirTodo.setText(
                if (expandidas.size == recomendaciones.size) R.string.propuesta_contraer_todo else R.string.propuesta_expandir_todo,
            )
        }
        return t.root
    }

    private fun aplicarEstado(t: ItemInfoPreventivaBinding, id: Int) {
        val abierta = id in expandidas
        t.detalle.visibility = if (abierta) View.VISIBLE else View.GONE
        t.flecha.rotation = if (abierta) 180f else 0f
    }

    /** RF24: si falla la consulta del repositorio, se avisa con el mensaje del ERS. */
    private fun mostrarError() {
        binding.contenido.visibility = View.GONE
        binding.textoError.visibility = View.VISIBLE
    }

    companion object {
        private const val EXTRA_MASCOTA_ID = "mascota_id"

        fun intent(contexto: Context, mascotaId: Int) =
            Intent(contexto, InfoPreventivaActivity::class.java).putExtra(EXTRA_MASCOTA_ID, mascotaId)
    }
}
