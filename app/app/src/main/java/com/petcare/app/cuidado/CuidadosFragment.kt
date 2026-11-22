package com.petcare.app.cuidado

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.TypefaceSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
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
import com.petcare.app.notificacion.AvisoNotificaciones
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Sección Cuidados (RF23): los cuidados de una mascota con su historial, filtrados por estado,
 * y las acciones para agregar (RF12), editar (RF14) y eliminar (RF15) un cuidado.
 */
class CuidadosFragment : Fragment() {

    // Franja "notificaciones desactivadas": se muestra sola mientras falte el permiso.
    private val avisoNotificaciones = AvisoNotificaciones(this)

    private var _binding: FragmentCuidadosBinding? = null
    private val binding get() = _binding!!
    private val mascotas = MascotaRepository()
    private val repositorio = CuidadoRepository()

    private var activas: List<MascotaRespuesta> = emptyList()
    private var inactivaVisible = 0 // mascota inactiva pedida desde su perfil (0 = ninguna)
    private var mascotaId = 0
    private var filtro = TODOS
    private var cuidadoAEnfocar = 0 // cuidado al que hay que bajar al dibujar la lista (0 = ninguno)
    private var posicionAConservar: Int? = null // scroll a devolver tras recargar por una acción (null = ninguno)
    private var lista: List<CuidadoRespuesta> = emptyList()
    private var conteos = ConteoCuidados(0, 0, 0)

    // Qué tarjetas están abiertas y en cuáles se desplegaron los detalles (por id de cuidado).
    private val abiertos = mutableSetOf<Int>()
    private val conDetalles = mutableSetOf<Int>()
    private val historialCompleto = mutableSetOf<Int>() // cuidados cuyo historial se muestra entero

    override fun onCreateView(inflater: LayoutInflater, contenedor: ViewGroup?, estado: Bundle?): View {
        _binding = FragmentCuidadosBinding.inflate(inflater, contenedor, false)
        binding.root.respetarBarraDeEstado()

        binding.pestanaTodos.setOnClickListener { elegirFiltro(TODOS) }
        binding.pestanaPendientes.setOnClickListener { elegirFiltro(PENDIENTES) }
        binding.pestanaFinalizados.setOnClickListener { elegirFiltro(FINALIZADOS) }
        binding.campoBusqueda.doAfterTextChanged { dibujarLista() }
        binding.botonAgregar.setOnClickListener { mostrarHojaAgregar() }
        avisoNotificaciones.vincular(binding.avisoNotificaciones)
        return binding.root
    }

    // Se vuelve a cargar al regresar de registrar, agregar o editar algo.
    override fun onResume() {
        super.onResume()
        avisoNotificaciones.actualizar() // por si se activaron las notificaciones desde los ajustes
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

            // Una mascota inactiva solo aparece aquí cuando se llega desde su perfil ("Ver cuidados", RF23).
            val pedida = SeleccionCuidados.mascotaId
            SeleccionCuidados.mascotaId = 0
            // Si se llega desde el Cronograma, ese cuidado se abre desplegado y se muestra entre "Todos".
            val cuidadoPedido = SeleccionCuidados.cuidadoId
            SeleccionCuidados.cuidadoId = 0
            if (cuidadoPedido != 0) {
                abiertos.add(cuidadoPedido)
                cuidadoAEnfocar = cuidadoPedido
                filtro = TODOS
            }
            if (pedida != 0 && todas.any { it.id == pedida && it.estaInactiva }) inactivaVisible = pedida
            activas = todas.filter { !it.estaInactiva || it.id == inactivaVisible }

            // Sin mascotas activas se muestra el aviso, igual que en Mascotas y Cronograma.
            val sinMascotas = activas.isEmpty() && vista.vacio.configurarSiNoHayMascotas(todas) {
                startActivity(Intent(requireContext(), MascotaFormActivity::class.java))
            }
            vista.vacio.root.visibility = if (sinMascotas) View.VISIBLE else View.GONE
            vista.contenido.visibility = if (sinMascotas) View.GONE else View.VISIBLE
            vista.botonAgregar.visibility = if (sinMascotas) View.GONE else View.VISIBLE
            if (sinMascotas) return@launch

            mascotaId = when {
                pedida != 0 && activas.any { it.id == pedida } -> pedida
                activas.any { it.id == mascotaId } -> mascotaId
                else -> (activas.firstOrNull { !it.estaInactiva } ?: activas.first()).id
            }
            dibujarSelector()
            cargarCuidados()
        }
    }

    private fun dibujarSelector() {
        val contenedor = binding.selectorMascotas
        contenedor.removeAllViews()
        // A una mascota inactiva no se le agregan cuidados: solo se consultan.
        binding.botonAgregar.visibility =
            if (activas.firstOrNull { it.id == mascotaId }?.estaInactiva == true) View.GONE else View.VISIBLE
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
                    historialCompleto.clear()
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

    /** Recarga tras completar, anular o editar algo, y deja la pantalla donde estaba. */
    private fun recargarConservandoScroll() {
        posicionAConservar = _binding?.scrollCuidados?.scrollY
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
            posicionAConservar = null
            mostrarVacio(mascota, buscando = texto.isNotEmpty())
            return
        }

        vista.vacioLista.root.visibility = View.GONE
        val enfocar = cuidadoAEnfocar
        cuidadoAEnfocar = 0
        val posicion = posicionAConservar
        posicionAConservar = null
        // Al redibujar la lista el scroll podía saltar; se devuelve a donde estaba.
        if (posicion != null && enfocar == 0) {
            vista.scrollCuidados.post { _binding?.scrollCuidados?.scrollTo(0, posicion) }
        }
        visibles.forEach { cuidado ->
            val tarjeta = crearTarjeta(cuidado, mascota)
            vista.listaCuidados.addView(tarjeta)
            // Baja hasta el cuidado pedido para que no quede fuera de pantalla.
            if (cuidado.id == enfocar) {
                tarjeta.post { _binding?.scrollCuidados?.smoothScrollTo(0, vista.listaCuidados.top + tarjeta.top) }
            }
        }
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

        t.valorProximaFecha.text = if (c.estaFinalizado) getString(R.string.cuidado_estado_finalizado) else c.fechaRecurrente.comoFechaLegible()
        t.valorFrecuencia.text = c.textoFrecuencia(ctx)
        // "Duración: Se repite sin fecha final", con el subtítulo en negrita.
        t.valorDuracion.text = conSubtitulo(getString(R.string.cuidado_duracion), c.textoRepeticion(ctx))
        t.valorAviso.text = conSubtitulo(
            getString(R.string.cuidado_aviso_titulo),
            getString(if (c.avisoAnticipado) R.string.cuidado_aviso_frase_activado else R.string.cuidado_aviso_frase_desactivado),
        )

        t.cajaDescripcion.visibility = if (c.descripcion.isBlank()) View.GONE else View.VISIBLE
        t.valorDescripcion.text = c.descripcion
        t.cajaImportancia.visibility = if (c.importancia.isNullOrBlank()) View.GONE else View.VISIBLE
        t.valorImportancia.text = c.importancia.orEmpty()
        t.cajaNota.visibility = if (c.nota.isNullOrBlank()) View.GONE else View.VISIBLE
        t.valorNota.text = c.nota.orEmpty()

        t.botonCompletar.visibility = if (c.estaFinalizado || mascota.estaInactiva) View.GONE else View.VISIBLE
        t.avisoFinalizado.visibility = if (c.estaFinalizado) View.VISIBLE else View.GONE

        dibujarHistorial(t, c)
        aplicarEstadoDeTarjeta(t, c)

        t.cabecera.setOnClickListener {
            if (!abiertos.remove(c.id)) abiertos.add(c.id)
            aplicarEstadoDeTarjeta(t, c)
        }
        t.verDetalles.setOnClickListener {
            if (!conDetalles.remove(c.id)) conDetalles.add(c.id)
            aplicarEstadoDeTarjeta(t, c)
        }
        t.botonCompletar.setOnClickListener { iniciarCompletar(c) }
        t.botonOpciones.setOnClickListener { mostrarHojaDelCuidado(c) }
        return t.root
    }

    private fun dibujarHistorial(t: ItemCuidadoBinding, c: CuidadoRespuesta) {
        t.tituloHistorial.text = getString(R.string.cuidado_historial_cantidad, c.realizadas)
        t.historial.removeAllViews()
        t.sinHistorial.visibility = if (c.actividades.isEmpty()) View.VISIBLE else View.GONE

        // Se muestran las últimas actividades; con más de MAXIMO_ACTIVIDADES hay un botón para ver todas.
        val excede = c.actividades.size > MAXIMO_ACTIVIDADES
        val completo = c.id in historialCompleto
        val mostradas = if (excede && !completo) c.actividades.take(MAXIMO_ACTIVIDADES) else c.actividades
        t.verTodasActividades.visibility = if (excede) View.VISIBLE else View.GONE
        t.verTodasActividades.text =
            if (completo) getString(R.string.cuidado_historial_ver_menos)
            else getString(R.string.cuidado_historial_ver_todas, c.actividades.size)
        t.verTodasActividades.setOnClickListener {
            if (!historialCompleto.remove(c.id)) historialCompleto.add(c.id)
            dibujarHistorial(t, c)
        }

        mostradas.forEach { actividad ->
            val fila = ItemActividadBinding.inflate(layoutInflater, t.historial, false)
            fila.fecha.text = actividad.fechaRealizacion.comoFechaLegible()
            // Las actividades anuladas no llegan: el historial solo muestra las completadas.
            etiquetar(fila.estado, R.string.cuidado_actividad_completada, R.color.petcare_brand_100, R.color.petcare_primary_dark)
            fila.nota.visibility = if (actividad.nota.isNullOrBlank()) View.GONE else View.VISIBLE
            fila.nota.text = getString(R.string.cuidado_actividad_nota, actividad.nota.orEmpty())
            fila.fila.setOnClickListener { mostrarHojaDeActividad(actividad) }
            t.historial.addView(fila.root)
        }
    }

    /** "Subtítulo: texto" en una sola línea, con el subtítulo en la misma letra que "Descripción". */
    private fun conSubtitulo(subtitulo: String, texto: String) = buildSpannedString {
        // Igual que los títulos "Descripción" e "Importancia": seminegrita, 11sp (12sp del texto) y gris
        val letra = ResourcesCompat.getFont(requireContext(), R.font.poppins_semibold)
        val gris = ContextCompat.getColor(requireContext(), R.color.petcare_text_muted)
        inSpans(ForegroundColorSpan(gris), RelativeSizeSpan(11f / 12f)) {
            if (letra != null) {
                inSpans(TypefaceSpan(letra)) { append("$subtitulo:") }
            } else {
                bold { append("$subtitulo:") }
            }
        }
        append(" ")
        append(texto)
    }

    /** Refleja si la tarjeta está abierta y si tiene los detalles desplegados. */
    private fun aplicarEstadoDeTarjeta(t: ItemCuidadoBinding, c: CuidadoRespuesta) {
        val abierta = c.id in abiertos
        val detalles = c.id in conDetalles
        // Abierta, la fecha y las veces realizadas ya se ven abajo (cuadro "Próxima fecha" y título del
        // historial); solo se conserva el aviso "Venció hace N días" cuando el cuidado está vencido.
        t.realizado.visibility = if (abierta) View.GONE else View.VISIBLE
        t.proxima.visibility = when {
            c.estaFinalizado -> View.GONE
            abierta && c.diasVencido() <= 0 -> View.GONE
            else -> View.VISIBLE
        }
        t.botonOpciones.visibility = if (abierta) View.VISIBLE else View.GONE // los 3 puntitos, solo con el cuidado abierto
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
                OpcionHoja(R.drawable.ic_escudo, R.string.hoja_agregar_recomendacion) {
                    startActivity(InfoPreventivaActivity.intent(requireContext(), mascota.id))
                },
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
                    if (_binding != null) recargarConservandoScroll()
                }
                is Resultado.Fallo -> avisar(resultado.paraMostrar(requireContext()))
            }
        }
    }

    // ---------- Completar un cuidado y el historial (RF16 a RF19) ----------

    /**
     * RF16: pregunta cuándo se realizó. RF17: si el cuidado se repite y la fecha difiere de la
     * planificada, pregunta además qué fecha usar como base para calcular la próxima.
     */
    private fun iniciarCompletar(c: CuidadoRespuesta) {
        val contexto = requireContext()
        contexto.dialogoCompletar(c) { fecha, nota ->
            val planificada = LocalDate.parse(c.fechaRecurrente)
            if (c.modoTermino != "una_vez" && fecha != planificada) {
                contexto.dialogoFechaBase(c, planificada, fecha) { base -> completar(c, fecha, nota, base) }
            } else {
                completar(c, fecha, nota, null)
            }
        }
    }

    private fun completar(c: CuidadoRespuesta, fecha: LocalDate, nota: String?, base: String?) {
        viewLifecycleOwner.lifecycleScope.launch {
            when (val resultado = repositorio.completar(c.id, CompletarPeticion(fecha.toString(), nota, base))) {
                is Resultado.Exito -> {
                    val cuidado = resultado.dato.cuidado
                    avisar(
                        if (cuidado.estaFinalizado) getString(R.string.actividad_registrada)
                        else getString(R.string.actividad_registrada_proxima, cuidado.fechaRecurrente.comoFechaLegible()),
                    )
                    if (_binding != null) recargarConservandoScroll()
                }
                is Resultado.Fallo -> avisar(resultado.paraMostrar(requireContext()))
            }
        }
    }

    /** Tocar una actividad del historial: agregar, editar o quitar su nota, o anularla. */
    private fun mostrarHojaDeActividad(a: ActividadRespuesta) {
        val tieneNota = !a.nota.isNullOrBlank()
        val opciones = mutableListOf(
            OpcionHoja(
                R.drawable.ic_editar,
                if (tieneNota) R.string.actividad_editar_nota else R.string.actividad_agregar_nota,
            ) { editarNota(a) },
        )
        if (tieneNota) {
            opciones += OpcionHoja(R.drawable.ic_eliminar, R.string.actividad_eliminar_nota, peligro = true) { confirmarEliminarNota(a) }
        }
        opciones += OpcionHoja(R.drawable.ic_eliminar, R.string.actividad_anular, peligro = true) { confirmarAnulacion(a) }
        requireContext().mostrarHojaOpciones(a.fechaRealizacion.comoFechaLegible(), opciones)
    }

    private fun editarNota(a: ActividadRespuesta) {
        requireContext().dialogoNota(a.nota) { nota -> ejecutar { repositorio.guardarNota(a.id, nota) } }
    }

    /** RF18: pide confirmación; si se cancela, la nota se mantiene. */
    private fun confirmarEliminarNota(a: ActividadRespuesta) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.actividad_eliminar_nota_titulo)
            .setMessage(R.string.actividad_eliminar_nota_mensaje)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.actividad_eliminar_nota) { _, _ -> ejecutar { repositorio.eliminarNota(a.id) } }
            .show()
    }

    /** RF19: pide confirmación; si se cancela, la actividad se mantiene en el historial. */
    private fun confirmarAnulacion(a: ActividadRespuesta) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.actividad_anular_titulo)
            .setMessage(R.string.actividad_anular_mensaje)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.actividad_anular_boton) { _, _ -> ejecutar { repositorio.anular(a.id) } }
            .show()
    }

    /** Ejecuta una acción sobre una actividad, avisa con el mensaje del servidor y recarga la lista. */
    private fun ejecutar(accion: suspend () -> Resultado<GuardarCuidadoRespuesta>) {
        viewLifecycleOwner.lifecycleScope.launch {
            when (val resultado = accion()) {
                is Resultado.Exito -> {
                    avisar(resultado.dato.mensaje)
                    if (_binding != null) recargarConservandoScroll()
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
        avisoNotificaciones.desvincular()
        _binding = null
    }

    private companion object {
        const val TODOS = "todos"
        const val PENDIENTES = "pendientes"
        const val FINALIZADOS = "finalizados"
        const val MAXIMO_ACTIVIDADES = 5 // actividades del historial que se ven antes de "Ver todas"
    }
}
