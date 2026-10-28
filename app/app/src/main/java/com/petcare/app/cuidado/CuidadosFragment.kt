package com.petcare.app.cuidado

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.petcare.app.R
import com.petcare.app.common.OpcionHoja
import com.petcare.app.common.Resultado
import com.petcare.app.common.configurar
import com.petcare.app.common.mostrarHojaOpciones
import com.petcare.app.common.paraMostrar
import com.petcare.app.common.respetarBarraDeEstado
import com.petcare.app.databinding.FragmentCuidadosBinding
import com.petcare.app.databinding.ItemActividadBinding
import com.petcare.app.databinding.ItemCuidadoBinding
import com.petcare.app.databinding.ItemSelectorMascotaBinding
import com.petcare.app.mascota.Especies
import com.petcare.app.mascota.MascotaFormActivity
import com.petcare.app.mascota.MascotaRepository
import com.petcare.app.mascota.MascotaRespuesta
import com.petcare.app.mascota.comoFechaLegible
import com.petcare.app.mascota.configurarSiNoHayMascotas
import com.petcare.app.mascota.mostrarMascota
import kotlinx.coroutines.launch

/**
 * Sección Cuidados (RF23): los cuidados de una mascota con su historial, filtrados por estado,
 * y las acciones para agregar (RF12), editar (RF14) y eliminar (RF15) un cuidado.
 */
class CuidadosFragment : Fragment() {

    private var _binding: FragmentCuidadosBinding? = null
    private val binding get() = _binding!!
    private val mascotas = MascotaRepository()
    private val repositorio = CuidadoRepository()

    private var activas: List<MascotaRespuesta> = emptyList()
    private var mascotaId = 0
    private var filtro = TODOS
    private var lista: List<CuidadoRespuesta> = emptyList()
    private var conteos = ConteoCuidados(0, 0, 0)

    // Qué tarjetas están abiertas y en cuáles se desplegaron los detalles (por id de cuidado).
    private val abiertos = mutableSetOf<Int>()
    private val conDetalles = mutableSetOf<Int>()

    override fun onCreateView(inflater: LayoutInflater, contenedor: ViewGroup?, estado: Bundle?): View {
        _binding = FragmentCuidadosBinding.inflate(inflater, contenedor, false)
        binding.root.respetarBarraDeEstado()

        binding.pestanaTodos.setOnClickListener { elegirFiltro(TODOS) }
        binding.pestanaPendientes.setOnClickListener { elegirFiltro(PENDIENTES) }
        binding.pestanaFinalizados.setOnClickListener { elegirFiltro(FINALIZADOS) }
        binding.campoBusqueda.doAfterTextChanged { dibujarLista() }
        binding.botonAgregar.setOnClickListener { mostrarHojaAgregar() }
        return binding.root
    }

    // Se vuelve a cargar al regresar de registrar, agregar o editar algo.
    override fun onResume() {
        super.onResume()
        cargarMascotas()
    }

    // ---------- Mascotas ----------

    private fun cargarMascotas() {
        viewLifecycleOwner.lifecycleScope.launch {
            val resultado = mascotas.listar()
            val vista = _binding ?: return@launch
            if (resultado is Resultado.Fallo) {
                avisar(resultado.paraMostrar(requireContext()))
                return@launch
            }
            val todas = (resultado as Resultado.Exito).dato
            activas = todas.filter { !it.estaInactiva }

            // Sin mascotas activas se muestra el aviso, igual que en Mascotas y Cronograma.
            val sinMascotas = vista.vacio.configurarSiNoHayMascotas(todas) {
                startActivity(Intent(requireContext(), MascotaFormActivity::class.java))
            }
            vista.vacio.root.visibility = if (sinMascotas) View.VISIBLE else View.GONE
            vista.contenido.visibility = if (sinMascotas) View.GONE else View.VISIBLE
            vista.botonAgregar.visibility = if (sinMascotas) View.GONE else View.VISIBLE
            if (sinMascotas) return@launch

            val pedida = SeleccionCuidados.mascotaId
            SeleccionCuidados.mascotaId = 0
            mascotaId = when {
                pedida != 0 && activas.any { it.id == pedida } -> pedida
                activas.any { it.id == mascotaId } -> mascotaId
                else -> activas.first().id
            }
            dibujarSelector()
            cargarCuidados()
        }
    }

    private fun dibujarSelector() {
        val contenedor = binding.selectorMascotas
        contenedor.removeAllViews()
        activas.forEach { mascota ->
            val item = ItemSelectorMascotaBinding.inflate(layoutInflater, contenedor, false)
            val elegida = mascota.id == mascotaId
            item.foto.mostrarMascota(mascota)
            item.foto.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), Especies.color(mascota.especie)))
            item.anillo.isSelected = elegida
            item.nombre.text = mascota.nombre
            item.nombre.setTextColor(color(if (elegida) R.color.petcare_text else R.color.petcare_text_muted))
            item.nombre.typeface = ResourcesCompat.getFont(requireContext(), if (elegida) R.font.poppins_semibold else R.font.poppins_regular)
            item.root.setOnClickListener {
                if (mascota.id != mascotaId) {
                    mascotaId = mascota.id
                    abiertos.clear()
                    conDetalles.clear()
                    dibujarSelector()
                    cargarCuidados()
                }
            }
            contenedor.addView(item.root)
        }
    }

    // ---------- Cuidados ----------

    private fun elegirFiltro(nuevo: String) {
        if (nuevo == filtro) return
        filtro = nuevo
        cargarCuidados()
    }

    private fun cargarCuidados() {
        val id = mascotaId
        val estado = filtro
        viewLifecycleOwner.lifecycleScope.launch {
            val resultado = repositorio.listar(id, estado)
            if (_binding == null || id != mascotaId || estado != filtro) return@launch // se eligió otra cosa mientras cargaba
            when (resultado) {
                is Resultado.Exito -> {
                    lista = resultado.dato.cuidados
                    conteos = resultado.dato.conteos
                    actualizarPestanas()
                    dibujarLista()
                }
                is Resultado.Fallo -> avisar(resultado.paraMostrar(requireContext()))
            }
        }
    }

    private fun actualizarPestanas() {
        binding.contadorTodos.text = conteos.todos.toString()
        binding.contadorPendientes.text = conteos.pendientes.toString()
        binding.contadorFinalizados.text = conteos.finalizados.toString()
        binding.pestanaTodos.isSelected = filtro == TODOS
        binding.pestanaPendientes.isSelected = filtro == PENDIENTES
        binding.pestanaFinalizados.isSelected = filtro == FINALIZADOS
    }

    private fun dibujarLista() {
        val vista = _binding ?: return
        val mascota = activas.firstOrNull { it.id == mascotaId } ?: return
        vista.listaCuidados.removeAllViews()
        vista.bloqueFiltros.visibility = if (conteos.todos == 0) View.GONE else View.VISIBLE

        val texto = vista.campoBusqueda.text.toString().trim()
        val visibles = lista.filter { texto.isEmpty() || it.nombre.contains(texto, ignoreCase = true) }
        if (visibles.isEmpty()) {
            mostrarVacio(mascota, buscando = texto.isNotEmpty())
            return
        }

        vista.vacioLista.root.visibility = View.GONE
        visibles.forEach { vista.listaCuidados.addView(crearTarjeta(it, mascota)) }
    }

    /** Qué decir cuando no hay nada que listar: sin cuidados, sin resultados o un estado vacío. */
    private fun mostrarVacio(mascota: MascotaRespuesta, buscando: Boolean) {
        val vacio = binding.vacioLista
        when {
            conteos.todos == 0 -> {
                vacio.configurar(
                    R.drawable.ic_corazon,
                    R.string.sin_cuidados_titulo,
                    R.string.sin_cuidados_mensaje,
                    R.string.boton_ver_recomendados,
                ) { abrirPropuesta(mascota) }
            }
            buscando -> textoVacio(R.drawable.ic_buscar, getString(R.string.sin_resultados_titulo), getString(R.string.sin_resultados_mensaje))
            filtro == FINALIZADOS -> textoVacio(
                R.drawable.ic_check,
                getString(R.string.sin_finalizados_titulo),
                getString(R.string.sin_finalizados_mensaje, mascota.nombre),
            )
            else -> textoVacio(
                R.drawable.ic_check,
                getString(R.string.sin_pendientes_titulo),
                getString(R.string.sin_pendientes_mensaje, mascota.nombre),
            )
        }
        vacio.boton.visibility = if (conteos.todos == 0) View.VISIBLE else View.GONE
        vacio.root.visibility = View.VISIBLE
    }

    private fun textoVacio(@DrawableRes icono: Int, titulo: String, mensaje: String) {
        val vacio = binding.vacioLista
        vacio.icono.setImageResource(icono)
        vacio.titulo.text = titulo
        vacio.mensaje.text = mensaje
    }

    // ---------- Tarjeta de un cuidado ----------

    private fun crearTarjeta(c: CuidadoRespuesta, mascota: MascotaRespuesta): View {
        val ctx = requireContext()
        val t = ItemCuidadoBinding.inflate(layoutInflater, binding.listaCuidados, false)

        t.fotoMascota.mostrarMascota(mascota)
        t.fotoMascota.backgroundTintList = ColorStateList.valueOf(color(Especies.color(mascota.especie)))
        t.nombre.text = c.nombre
        t.realizado.text = c.textoRealizado(ctx)

        // Estado: pendiente, vencido (pendiente cuya fecha ya pasó) o finalizado.
        val vencido = c.diasVencido()
        when {
            c.estaFinalizado -> {
                t.proxima.visibility = View.GONE
                etiquetar(t.estado, R.string.cuidado_estado_finalizado, R.color.petcare_border, R.color.petcare_text_muted)
            }
            vencido > 0 -> {
                t.proxima.text = resources.getQuantityString(R.plurals.cuidado_vencio, vencido.toInt(), vencido.toInt())
                t.proxima.setTextColor(color(R.color.petcare_error))
                etiquetar(t.estado, R.string.cuidado_estado_pendiente, R.color.petcare_danger_100, R.color.petcare_error)
            }
            else -> {
                t.proxima.text = getString(R.string.cuidado_proxima, c.fechaRecurrente.comoFechaCorta())
                etiquetar(t.estado, R.string.cuidado_estado_pendiente, R.color.petcare_brand_100, R.color.petcare_primary_dark)
            }
        }

        t.valorProximaFecha.text = if (c.estaFinalizado) getString(R.string.dato_vacio) else c.fechaRecurrente.comoFechaLegible()
        t.valorFrecuencia.text = c.textoFrecuencia(ctx)
        t.valorDuracion.text = c.textoDuracion(ctx)
        t.valorAviso.setText(if (c.avisoAnticipado) R.string.cuidado_activado else R.string.cuidado_desactivado)

        t.cajaDescripcion.visibility = if (c.descripcion.isBlank()) View.GONE else View.VISIBLE
        t.valorDescripcion.text = c.descripcion
        t.cajaImportancia.visibility = if (c.importancia.isNullOrBlank()) View.GONE else View.VISIBLE
        t.valorImportancia.text = c.importancia.orEmpty()
        t.cajaNota.visibility = if (c.nota.isNullOrBlank()) View.GONE else View.VISIBLE
        t.valorNota.text = c.nota.orEmpty()

        t.botonCompletar.visibility = if (c.estaFinalizado) View.GONE else View.VISIBLE
        t.avisoFinalizado.visibility = if (c.estaFinalizado) View.VISIBLE else View.GONE

        dibujarHistorial(t, c)
        aplicarEstadoDeTarjeta(t, c.id)

        t.cabecera.setOnClickListener {
            if (!abiertos.remove(c.id)) abiertos.add(c.id)
            aplicarEstadoDeTarjeta(t, c.id)
        }
        t.verDetalles.setOnClickListener {
            if (!conDetalles.remove(c.id)) conDetalles.add(c.id)
            aplicarEstadoDeTarjeta(t, c.id)
        }
        // Marcar como completado y el historial de actividades llegan en el siguiente paso.
        t.botonCompletar.setOnClickListener { avisar(getString(R.string.proximamente)) }
        t.botonOpciones.setOnClickListener { mostrarHojaDelCuidado(c) }
        return t.root
    }

    private fun dibujarHistorial(t: ItemCuidadoBinding, c: CuidadoRespuesta) {
        t.historial.removeAllViews()
        t.sinHistorial.visibility = if (c.actividades.isEmpty()) View.VISIBLE else View.GONE
        c.actividades.forEach { actividad ->
            val fila = ItemActividadBinding.inflate(layoutInflater, t.historial, false)
            fila.fecha.text = actividad.fechaRealizacion.comoFechaLegible()
            if (actividad.estado == "anulada") {
                etiquetar(fila.estado, R.string.cuidado_actividad_anulada, R.color.petcare_border, R.color.petcare_text_muted)
            } else {
                etiquetar(fila.estado, R.string.cuidado_actividad_completada, R.color.petcare_brand_100, R.color.petcare_primary_dark)
            }
            fila.nota.visibility = if (actividad.nota.isNullOrBlank()) View.GONE else View.VISIBLE
            fila.nota.text = getString(R.string.cuidado_actividad_nota, actividad.nota.orEmpty())
            t.historial.addView(fila.root)
        }
    }

    /** Refleja si la tarjeta está abierta y si tiene los detalles desplegados. */
    private fun aplicarEstadoDeTarjeta(t: ItemCuidadoBinding, id: Int) {
        val abierta = id in abiertos
        val detalles = id in conDetalles
        t.detalle.visibility = if (abierta) View.VISIBLE else View.GONE
        t.flecha.rotation = if (abierta) 180f else 0f
        t.bloqueDetalles.visibility = if (detalles) View.VISIBLE else View.GONE
        t.flechaDetalles.rotation = if (detalles) 180f else 0f
        t.textoVerDetalles.setText(if (detalles) R.string.cuidado_ocultar_detalles else R.string.cuidado_ver_detalles)
    }

    /** Pinta una etiqueta redondeada con su texto y sus colores. */
    private fun etiquetar(vista: TextView, @StringRes texto: Int, @ColorRes fondo: Int, @ColorRes letra: Int) {
        vista.setText(texto)
        vista.backgroundTintList = ColorStateList.valueOf(color(fondo))
        vista.setTextColor(color(letra))
    }

    // ---------- Acciones ----------

    private fun mostrarHojaAgregar() {
        val mascota = activas.firstOrNull { it.id == mascotaId } ?: return
        requireContext().mostrarHojaOpciones(
            getString(R.string.hoja_agregar_titulo),
            listOf(
                OpcionHoja(R.drawable.ic_editar, R.string.hoja_agregar_personalizado) {
                    startActivity(CuidadoFormActivity.intentNuevo(requireContext(), mascota.id))
                },
                OpcionHoja(R.drawable.ic_escudo, R.string.hoja_agregar_recomendacion) { abrirPropuesta(mascota) },
            ),
        )
    }

    private fun mostrarHojaDelCuidado(c: CuidadoRespuesta) {
        requireContext().mostrarHojaOpciones(
            c.nombre,
            listOf(
                OpcionHoja(R.drawable.ic_editar, R.string.hoja_editar) {
                    startActivity(CuidadoFormActivity.intentEditar(requireContext(), c.id))
                },
                OpcionHoja(R.drawable.ic_eliminar, R.string.hoja_eliminar, peligro = true) { confirmarEliminacion(c) },
            ),
        )
    }

    /** RF15: pide confirmación; si se cancela, el cuidado se mantiene. */
    private fun confirmarEliminacion(c: CuidadoRespuesta) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.cuidado_eliminar_titulo)
            .setMessage(R.string.cuidado_eliminar_mensaje)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.hoja_eliminar) { _, _ -> eliminar(c) }
            .show()
    }

    private fun eliminar(c: CuidadoRespuesta) {
        viewLifecycleOwner.lifecycleScope.launch {
            when (val resultado = repositorio.eliminar(c.id)) {
                is Resultado.Exito -> {
                    avisar(getString(R.string.cuidado_eliminado))
                    abiertos.remove(c.id)
                    conDetalles.remove(c.id)
                    if (_binding != null) cargarCuidados()
                }
                is Resultado.Fallo -> avisar(resultado.paraMostrar(requireContext()))
            }
        }
    }

    private fun abrirPropuesta(mascota: MascotaRespuesta) {
        startActivity(PropuestaActivity.intent(requireContext(), mascota.id))
    }

    private fun color(@ColorRes id: Int) = ContextCompat.getColor(requireContext(), id)

    private fun avisar(mensaje: String) = Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val TODOS = "todos"
        const val PENDIENTES = "pendientes"
        const val FINALIZADOS = "finalizados"
    }
}
