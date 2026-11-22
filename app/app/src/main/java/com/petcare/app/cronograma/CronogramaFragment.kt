package com.petcare.app.cronograma

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.petcare.app.MainActivity
import com.petcare.app.R
import com.petcare.app.common.Resultado
import com.petcare.app.common.SessionManager
import com.petcare.app.common.configurar
import com.petcare.app.common.paraMostrar
import com.petcare.app.common.respetarBarraDeEstado
import com.petcare.app.cuidado.CuidadoRepository
import com.petcare.app.cuidado.CuidadoRespuesta
import com.petcare.app.cuidado.PropuestaActivity
import com.petcare.app.cuidado.abrirCuidadosDe
import com.petcare.app.cuidado.mostrarCuidado
import com.petcare.app.databinding.FragmentCronogramaBinding
import com.petcare.app.databinding.ItemChipMascotaBinding
import com.petcare.app.databinding.ItemCronogramaBinding
import com.petcare.app.databinding.ItemDiaSemanaBinding
import com.petcare.app.mascota.Especies
import com.petcare.app.mascota.MascotaFormActivity
import com.petcare.app.mascota.MascotaRepository
import com.petcare.app.mascota.MascotaRespuesta
import com.petcare.app.mascota.comoFechaLegible
import com.petcare.app.mascota.configurarSiNoHayMascotas
import com.petcare.app.mascota.mostrarMascota
import com.petcare.app.notificacion.AvisoNotificaciones
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Sección Cronograma (RF13): los cuidados pendientes de todas las mascotas activas, agrupados por
 * vencidos, esta semana, próxima semana y más adelante. Se puede filtrar por mascota, por día y por nombre.
 */
class CronogramaFragment : Fragment() {

    // Franja "notificaciones desactivadas": se muestra sola mientras falte el permiso.
    private val avisoNotificaciones = AvisoNotificaciones(this)

    private var _binding: FragmentCronogramaBinding? = null
    private val binding get() = _binding!!
    private val mascotas = MascotaRepository()
    private val repositorio = CuidadoRepository()

    private var todasLasMascotas: List<MascotaRespuesta> = emptyList()
    private var activas: List<MascotaRespuesta> = emptyList()
    private var mascotaFiltro: Int? = null // null = todas
    private var lista: List<CuidadoRespuesta> = emptyList()
    private var diaElegido: LocalDate? = null

    override fun onCreateView(inflater: LayoutInflater, contenedor: ViewGroup?, estado: Bundle?): View {
        _binding = FragmentCronogramaBinding.inflate(inflater, contenedor, false)
        binding.cabecera.respetarBarraDeEstado()

        val nombre = SessionManager.nombre?.trim()?.substringBefore(' ').orEmpty()
        binding.saludo.text = getString(R.string.cronograma_saludo, nombre)
        binding.mes.text = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM yyyy", Locale("es", "PE"))).replace(".", "")

        binding.botonBuscar.setOnClickListener { alternarBusqueda() }
        binding.campoBusqueda.doAfterTextChanged { dibujarLista() }
        binding.mostrarTodo.setOnClickListener { elegirDia(null) }
        avisoNotificaciones.vincular(binding.avisoNotificaciones)
        return binding.root
    }

    // Se vuelve a cargar al regresar de registrar una mascota o de tocar un cuidado.
    override fun onResume() {
        super.onResume()
        avisoNotificaciones.actualizar() // por si se activaron las notificaciones desde los ajustes
        cargar()
    }

    private fun cargar() {
        viewLifecycleOwner.lifecycleScope.launch {
            val resultadoMascotas = mascotas.listar()
            if (_binding == null) return@launch
            if (resultadoMascotas is Resultado.Fallo) {
                avisar(resultadoMascotas.paraMostrar(requireContext()))
                return@launch
            }
            todasLasMascotas = (resultadoMascotas as Resultado.Exito).dato
            activas = todasLasMascotas.filter { !it.estaInactiva }
            if (activas.none { it.id == mascotaFiltro }) mascotaFiltro = null

            dibujarChips()
            val resultado = if (activas.isEmpty()) null else repositorio.cronograma(mascotaFiltro)
            if (_binding == null) return@launch
            when (resultado) {
                null -> lista = emptyList()
                is Resultado.Exito -> lista = resultado.dato.cuidados
                is Resultado.Fallo -> {
                    avisar(resultado.paraMostrar(requireContext()))
                    return@launch
                }
            }
            dibujarTodo()
        }
    }

    private fun dibujarTodo() {
        dibujarResumen()
        dibujarSemana()
        dibujarLista()
    }

    // ---------- Cabecera ----------

    private fun fecha(c: CuidadoRespuesta): LocalDate = LocalDate.parse(c.fechaRecurrente)

    /** "Tienes N cuidados para esta semana" (incluye los vencidos) y el siguiente cuidado. */
    private fun dibujarResumen() {
        val hoy = LocalDate.now()
        val domingo = hoy.with(DayOfWeek.SUNDAY)
        val estaSemana = lista.count { fecha(it) <= domingo }
        binding.titulo.text =
            if (estaSemana == 0) getString(R.string.cronograma_titulo)
            else resources.getQuantityString(R.plurals.cronograma_titulo_con_cuidados, estaSemana, estaSemana)

        val siguiente = lista.firstOrNull { fecha(it) >= hoy }
        if (siguiente == null) {
            binding.tarjetaResumen.visibility = View.GONE
            return
        }
        val mascota = activas.firstOrNull { it.id == siguiente.mascotaId }
        if (mascota != null) {
            binding.fotoSiguiente.mostrarMascota(mascota)
            binding.fotoSiguiente.backgroundTintList = ColorStateList.valueOf(color(Especies.color(mascota.especie)))
            binding.mascotaSiguiente.text = mascota.nombre
        }
        val dias = ChronoUnit.DAYS.between(hoy, fecha(siguiente)).toInt()
        binding.nombreSiguiente.text = siguiente.nombre
        binding.diasSiguiente.text = dias.toString()
        binding.unidadDias.text = resources.getQuantityString(R.plurals.cronograma_dias, dias)
        binding.tarjetaResumen.visibility = View.VISIBLE
    }

    // ---------- Semana ----------

    /** Los siete días de la semana actual: hoy resaltado, el día elegido en verde y un punto si tiene cuidados. */
    private fun dibujarSemana() {
        binding.semana.removeAllViews()
        val letras = listOf("L", "M", "X", "J", "V", "S", "D")
        val hoy = LocalDate.now()
        val lunes = hoy.with(DayOfWeek.MONDAY)
        for (i in 0..6) {
            val dia = lunes.plusDays(i.toLong())
            val item = ItemDiaSemanaBinding.inflate(layoutInflater, binding.semana, false)
            val elegido = dia == diaElegido
            item.letra.text = letras[i]
            item.numero.text = dia.dayOfMonth.toString()
            item.root.isSelected = dia == hoy
            item.root.isActivated = elegido
            item.punto.visibility = if (lista.any { fecha(it) == dia }) View.VISIBLE else View.INVISIBLE
            // El punto es blanco sobre el día elegido, que va en verde sólido.
            item.punto.backgroundTintList = if (elegido) ColorStateList.valueOf(color(R.color.white)) else null
            item.root.setOnClickListener { elegirDia(if (elegido) null else dia) }
            binding.semana.addView(item.root)
        }
    }

    private fun elegirDia(dia: LocalDate?) {
        diaElegido = dia
        dibujarSemana()
        dibujarLista()
    }

    // ---------- Filtros ----------

    private fun dibujarChips() {
        val contenedor = binding.chipsMascotas
        contenedor.removeAllViews()
        if (activas.isEmpty()) return

        val todas = ItemChipMascotaBinding.inflate(layoutInflater, contenedor, false)
        todas.foto.setImageResource(R.drawable.ic_huella)
        todas.foto.imageTintList = ColorStateList.valueOf(color(R.color.petcare_primary))
        todas.nombre.setText(R.string.filtro_todas)
        todas.root.isSelected = mascotaFiltro == null
        todas.root.setOnClickListener { elegirMascota(null) }
        contenedor.addView(todas.root)

        activas.forEach { mascota ->
            val chip = ItemChipMascotaBinding.inflate(layoutInflater, contenedor, false)
            chip.foto.mostrarMascota(mascota)
            chip.foto.backgroundTintList = ColorStateList.valueOf(color(Especies.color(mascota.especie)))
            chip.nombre.text = mascota.nombre
            chip.root.isSelected = mascotaFiltro == mascota.id
            chip.root.setOnClickListener { elegirMascota(mascota.id) }
            contenedor.addView(chip.root)
        }
    }

    private fun elegirMascota(id: Int?) {
        if (id == mascotaFiltro) return
        mascotaFiltro = id
        cargar()
    }

    private fun alternarBusqueda() {
        val abierta = binding.filaBusqueda.visibility == View.VISIBLE
        binding.filaBusqueda.visibility = if (abierta) View.GONE else View.VISIBLE
        if (abierta) binding.campoBusqueda.setText("") // al cerrarla se quita el filtro
    }

    // ---------- Lista ----------

    private fun dibujarLista() {
        val vista = _binding ?: return
        vista.lista.removeAllViews()
        vista.vacio.root.visibility = View.GONE

        val dia = diaElegido
        vista.filaDia.visibility = if (dia == null) View.GONE else View.VISIBLE
        if (dia != null) vista.tituloDia.text = dia.toString().comoFechaLegible()

        val texto = vista.campoBusqueda.text.toString().trim()
        var visibles = lista.filter { texto.isEmpty() || it.nombre.contains(texto, ignoreCase = true) }
        if (dia != null) visibles = visibles.filter { fecha(it) == dia }
        if (visibles.isEmpty()) {
            mostrarVacio(buscando = texto.isNotEmpty(), dia = dia)
            return
        }

        if (dia != null) {
            visibles.forEach { agregarFila(it) }
        } else {
            agruparPorFecha(visibles)
        }
    }

    /** VENCIDOS, ESTA SEMANA, PRÓXIMA SEMANA y MÁS ADELANTE, en ese orden. */
    private fun agruparPorFecha(cuidados: List<CuidadoRespuesta>) {
        val hoy = LocalDate.now()
        val finEstaSemana = hoy.with(DayOfWeek.SUNDAY)
        val finProximaSemana = finEstaSemana.plusWeeks(1)
        val grupos = listOf(
            R.string.cronograma_vencidos to cuidados.filter { fecha(it) < hoy },
            R.string.cronograma_esta_semana to cuidados.filter { fecha(it) in hoy..finEstaSemana },
            R.string.cronograma_proxima_semana to cuidados.filter { fecha(it) > finEstaSemana && fecha(it) <= finProximaSemana },
            R.string.cronograma_mas_adelante to cuidados.filter { fecha(it) > finProximaSemana },
        )
        grupos.filter { it.second.isNotEmpty() }.forEach { (titulo, items) ->
            binding.lista.addView(crearTituloDeSeccion(titulo))
            items.forEach { agregarFila(it) }
        }
    }

    private fun crearTituloDeSeccion(titulo: Int): View = TextView(requireContext()).apply {
        setText(titulo)
        setTextColor(color(R.color.petcare_text_muted))
        textSize = 12f
        letterSpacing = 0.05f
        typeface = androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.poppins_semibold)
        setPadding(0, dp(18), 0, dp(2))
    }

    private fun agregarFila(c: CuidadoRespuesta) {
        val fila = ItemCronogramaBinding.inflate(layoutInflater, binding.lista, false)
        fila.mostrarCuidado(c, activas.firstOrNull { it.id == c.mascotaId })
        fila.tarjeta.setOnClickListener { requireContext().abrirCuidadosDe(c.mascotaId, c.id) }
        binding.lista.addView(fila.root)
    }

    private fun mostrarVacio(buscando: Boolean, dia: LocalDate?) {
        val vacio = binding.vacio
        when {
            activas.isEmpty() -> {
                // Sin mascotas activas: mismo aviso que en Mascotas y Cuidados.
                vacio.configurarSiNoHayMascotas(todasLasMascotas) { startActivity(Intent(requireContext(), MascotaFormActivity::class.java)) }
                vacio.boton.visibility = View.VISIBLE
            }
            buscando -> textoVacio(R.drawable.ic_buscar, R.string.sin_resultados_titulo, R.string.sin_resultados_mensaje)
            dia != null -> textoVacio(R.drawable.ic_calendario, R.string.cronograma_dia_vacio_titulo, R.string.cronograma_dia_vacio_mensaje)
            else -> {
                vacio.configurar(
                    R.drawable.ic_corazon,
                    R.string.sin_cuidados_titulo,
                    R.string.sin_cuidados_mensaje,
                    R.string.boton_ver_recomendados,
                ) { verRecomendados() }
                vacio.boton.visibility = View.VISIBLE
            }
        }
        vacio.root.visibility = View.VISIBLE
    }

    private fun textoVacio(icono: Int, titulo: Int, mensaje: Int) {
        val vacio = binding.vacio
        vacio.icono.setImageResource(icono)
        vacio.titulo.setText(titulo)
        vacio.mensaje.setText(mensaje)
        vacio.boton.visibility = View.GONE
    }

    // ---------- Acciones ----------

    /** Con una sola mascota (o un filtro elegido) se abre directo; con varias, primero se elige para cuál. */
    private fun verRecomendados() {
        val candidatas = mascotaFiltro?.let { id -> activas.filter { it.id == id } } ?: activas
        if (candidatas.size == 1) {
            startActivity(PropuestaActivity.intent(requireContext(), candidatas.first().id))
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.elegir_mascota_titulo)
            .setItems(candidatas.map { it.nombre }.toTypedArray()) { _, posicion ->
                startActivity(PropuestaActivity.intent(requireContext(), candidatas[posicion].id))
            }
            .show()
    }

    private fun color(id: Int) = ContextCompat.getColor(requireContext(), id)

    private fun dp(valor: Int) = (valor * resources.displayMetrics.density).toInt()

    private fun avisar(mensaje: String) = Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        avisoNotificaciones.desvincular()
        _binding = null
    }
}
