package com.petcare.app.mapa

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.ChipGroup
import com.petcare.app.R
import com.petcare.app.common.Resultado
import com.petcare.app.common.UbicacionActual
import com.petcare.app.common.paraMostrar
import com.petcare.app.common.respetarBarraDeEstado
import com.petcare.app.databinding.FragmentMapaBinding
import com.petcare.app.databinding.HojaFiltrosMapaBinding
import com.petcare.app.databinding.HojaOrdenMapaBinding
import com.petcare.app.databinding.ItemOpcionOrdenBinding
import com.petcare.app.databinding.ItemResultadoMapaBinding
import com.petcare.app.mascota.Especies
import com.petcare.app.resena.ResenasCambiaron
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Pestaña Mapa (HU08), con el diseño del prototipo de Figma: veterinarias y campañas sobre un mapa de
 * OpenStreetMap, con búsqueda por nombre, categorías, listado, orden y filtros. Si no hay categoría ni
 * búsqueda se ven todos los marcadores. Los marcadores están en MarcadoresMapa.kt y los datos vienen de
 * MapaRepository.
 */
class MapaFragment : Fragment() {

    private var _binding: FragmentMapaBinding? = null
    private val binding get() = _binding!!
    private val repositorio = MapaRepository()
    private val adaptador = ResultadosAdapter()

    // Qué se está viendo
    private var categoria: Categoria? = null
    private var filtrosVeterinarias = FiltrosVeterinarias()
    private var filtrosCampanas = FiltrosCampanas()
    private var ordenVeterinarias: String? = null
    private var ordenCampanas: String? = null
    private var textoBusqueda = ""

    // Datos
    private var catalogos: CatalogosRespuesta? = null
    private var elementos: List<ElementoMapa> = emptyList()
    private var ubicacion: GeoPoint? = null
    private var trabajo: Job? = null

    // Estado de la pantalla
    private var ajustarVista = true
    private var panelExpandido = false
    private val marcadores = mutableListOf<Marker>()
    private var marcadorUbicacion: Marker? = null
    private var alTenerPermiso: ((Boolean) -> Unit)? = null

    private val pedirPermiso =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permisos ->
            val pendiente = alTenerPermiso
            alTenerPermiso = null
            if (permisos.values.any { it }) obtenerUbicacion(pendiente ?: {}) else pendiente?.invoke(false)
        }

    override fun onCreateView(inflater: LayoutInflater, contenedor: ViewGroup?, estado: Bundle?): View {
        _binding = FragmentMapaBinding.inflate(inflater, contenedor, false)
        prepararMapa()
        prepararControles()
        binding.encabezado.respetarBarraDeEstado()
        pintarCategorias()
        cargarCatalogos()
        ResenasCambiaron.consumir() // el mapa recién creado ya carga datos nuevos
        recargar()
        // La ubicación se pide al abrir el mapa: sirve para mostrar el punto azul y calcular distancias.
        pedirUbicacion { obtenida -> if (obtenida) recargar() }
        return binding.root
    }

    // ---------------------------------------------------------------------
    // Preparación
    // ---------------------------------------------------------------------

    private fun prepararMapa() {
        val aplicacion = requireContext().applicationContext
        // El caché de mapas queda en la carpeta privada de la app: no hace falta permiso de almacenamiento.
        Configuration.getInstance().apply {
            load(aplicacion, aplicacion.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            userAgentValue = aplicacion.packageName // OpenStreetMap exige identificar la app
            osmdroidBasePath = File(aplicacion.cacheDir, "osmdroid")
            osmdroidTileCache = File(osmdroidBasePath, "tiles")
        }
        binding.mapa.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(12.5)
            controller.setCenter(GeoPoint(-12.0464, -77.0428)) // Lima, mientras llegan los datos
            overlays.add(CopyrightOverlay(requireContext()))
        }
    }

    private fun prepararControles() {
        binding.lista.layoutManager = LinearLayoutManager(requireContext())
        binding.lista.adapter = adaptador

        binding.chipVeterinarias.setOnClickListener {
            elegirCategoria(if (categoria == Categoria.VETERINARIAS) null else Categoria.VETERINARIAS)
        }
        binding.chipCampanas.setOnClickListener {
            elegirCategoria(if (categoria == Categoria.CAMPANAS) null else Categoria.CAMPANAS)
        }

        binding.campoBusqueda.doAfterTextChanged { binding.limpiarBusqueda.isVisible = !it.isNullOrEmpty() }
        binding.campoBusqueda.setOnEditorActionListener { _, accion, _ ->
            if (accion == EditorInfo.IME_ACTION_SEARCH) {
                ocultarTeclado()
                buscar(binding.campoBusqueda.text.toString().trim())
                true
            } else {
                false
            }
        }
        binding.limpiarBusqueda.setOnClickListener {
            binding.campoBusqueda.text?.clear()
            ocultarTeclado()
            if (textoBusqueda.isNotEmpty()) buscar("")
        }

        binding.botonLeyenda.setOnClickListener { binding.leyenda.isVisible = !binding.leyenda.isVisible }
        binding.botonUbicacion.setOnClickListener { irAMiUbicacion() }
        binding.botonOrdenar.setOnClickListener { elegirOrden() }
        binding.botonFiltros.setOnClickListener { abrirFiltros() }
        binding.asa.setOnClickListener { alternarTamanoDelPanel() }
        binding.tituloLista.setOnClickListener { alternarTamanoDelPanel() }
    }

    private fun cargarCatalogos() {
        viewLifecycleOwner.lifecycleScope.launch {
            val resultado = repositorio.filtros()
            if (resultado is Resultado.Exito) catalogos = resultado.dato
        }
    }

    // ---------------------------------------------------------------------
    // Qué se muestra
    // ---------------------------------------------------------------------

    private fun elegirCategoria(nueva: Categoria?) {
        categoria = nueva
        if (nueva != null) {
            // Elegir una categoría deja atrás la búsqueda por nombre.
            textoBusqueda = ""
            binding.campoBusqueda.text?.clear()
        }
        pintarCategorias()
        ajustarVista = true
        recargar()
    }

    private fun buscar(texto: String) {
        textoBusqueda = texto
        if (texto.isNotEmpty()) categoria = null
        pintarCategorias()
        ajustarVista = true
        recargar()
    }

    /** La categoría elegida se ve en verde azulado con letra blanca; la otra, en blanco. */
    private fun pintarCategorias() {
        pintarCategoria(binding.chipVeterinarias, binding.iconoChipVeterinarias, binding.textoChipVeterinarias, categoria == Categoria.VETERINARIAS)
        pintarCategoria(binding.chipCampanas, binding.iconoChipCampanas, binding.textoChipCampanas, categoria == Categoria.CAMPANAS)
    }

    private fun pintarCategoria(contenedor: View, icono: ImageView, texto: TextView, activa: Boolean) {
        contenedor.setBackgroundResource(if (activa) R.drawable.bg_chip_mapa_activo else R.drawable.bg_chip_mapa)
        icono.imageTintList = ColorStateList.valueOf(color(if (activa) R.color.white else R.color.petcare_primary))
        texto.setTextColor(color(if (activa) R.color.white else R.color.petcare_text))
    }

    /** Vuelve a pedir al servidor lo que corresponde según la búsqueda, la categoría, los filtros y el orden. */
    private fun recargar() {
        trabajo?.cancel()
        trabajo = viewLifecycleOwner.lifecycleScope.launch {
            val lat = ubicacion?.latitude
            val lng = ubicacion?.longitude
            val resultado: Resultado<List<ElementoMapa>> = when {
                textoBusqueda.isNotEmpty() ->
                    repositorio.buscar(textoBusqueda, lat, lng).mapear { lista -> lista.map { it.comoElemento() } }

                categoria == Categoria.VETERINARIAS ->
                    repositorio.veterinarias(filtrosVeterinarias, ordenVeterinarias, lat, lng)
                        .mapear { lista -> lista.map { ElementoMapa.Veterinaria(it) } }

                categoria == Categoria.CAMPANAS ->
                    repositorio.campanas(filtrosCampanas, ordenCampanas, lat, lng)
                        .mapear { lista -> lista.map { ElementoMapa.Campana(it) } }

                else -> cargarTodo(lat, lng)
            }
            when (resultado) {
                is Resultado.Fallo -> avisar(resultado.paraMostrar(requireContext()))
                is Resultado.Exito -> {
                    elementos = resultado.dato
                    dibujarMarcadores()
                    actualizarPanel()
                    if (ajustarVista) {
                        ajustarMapa(elementos.map { GeoPoint(it.lat, it.lng) })
                        ajustarVista = false
                    }
                }
            }
        }
    }

    /** Sin categoría ni búsqueda: todas las veterinarias y las campañas que no están junto a una veterinaria. */
    private suspend fun cargarTodo(lat: Double?, lng: Double?): Resultado<List<ElementoMapa>> = coroutineScope {
        val veterinarias = async { repositorio.veterinarias(FiltrosVeterinarias(), null, lat, lng) }
        val campanas = async { repositorio.campanas(FiltrosCampanas(), null, lat, lng) }
        val v = veterinarias.await()
        val c = campanas.await()
        when {
            v is Resultado.Fallo -> v
            c is Resultado.Fallo -> c
            else -> {
                // Las campañas que están en la ubicación de su veterinaria ya se ven como insignia de la veterinaria.
                val soloVeterinarias = (v as Resultado.Exito).dato.map { ElementoMapa.Veterinaria(it) }
                val soloCampanas = (c as Resultado.Exito).dato
                    .filter { !it.enUbicacionDeVeterinaria }
                    .map { ElementoMapa.Campana(it) }
                Resultado.Exito(soloVeterinarias + soloCampanas)
            }
        }
    }

    private inline fun <T, R> Resultado<T>.mapear(transformar: (T) -> R): Resultado<R> = when (this) {
        is Resultado.Exito -> Resultado.Exito(transformar(dato))
        is Resultado.Fallo -> this
    }

    // ---------------------------------------------------------------------
    // Mapa
    // ---------------------------------------------------------------------

    private fun dibujarMarcadores() {
        val mapa = binding.mapa
        marcadores.forEach { mapa.overlays.remove(it) }
        marcadores.clear()
        val contexto = requireContext()
        for (elemento in elementos) {
            val marcador = Marker(mapa)
            marcador.position = GeoPoint(elemento.lat, elemento.lng)
            marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM) // la punta del globo marca el lugar
            marcador.title = elemento.nombre
            marcador.icon = when (elemento) {
                is ElementoMapa.Veterinaria ->
                    MarcadoresMapa.veterinaria(contexto, elemento.datos.tipo, elemento.datos.campanasAqui)
                is ElementoMapa.Campana -> MarcadoresMapa.campana(contexto, elemento.datos.estado)
            }
            marcador.setOnMarkerClickListener { _, _ ->
                seleccionar(elemento)
                true
            }
            mapa.overlays.add(marcador)
            marcadores.add(marcador)
        }
        // La ubicación del usuario queda por encima de los demás marcadores.
        marcadorUbicacion?.let {
            mapa.overlays.remove(it)
            mapa.overlays.add(it)
        }
        mapa.invalidate()
    }

    private fun ajustarMapa(puntos: List<GeoPoint>) {
        if (puntos.isEmpty()) return
        val mapa = binding.mapa
        if (puntos.size == 1) {
            mapa.controller.setZoom(16.0)
            mapa.controller.animateTo(puntos[0])
            return
        }
        val caja = BoundingBox.fromGeoPoints(puntos)
        // Si el mapa todavía no tiene tamaño, se espera a que lo tenga.
        mapa.post { _binding?.mapa?.zoomToBoundingBox(caja, true, 140) }
    }

    private fun mostrarMiUbicacion() {
        val punto = ubicacion ?: return
        val mapa = binding.mapa
        val marcador = marcadorUbicacion ?: Marker(mapa).also {
            it.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            it.icon = MarcadoresMapa.ubicacionActual(requireContext())
            it.title = getString(R.string.mapa_leyenda_ubicacion)
            it.setOnMarkerClickListener { _, _ -> true }
            marcadorUbicacion = it
        }
        marcador.position = punto
        mapa.overlays.remove(marcador)
        mapa.overlays.add(marcador)
        mapa.invalidate()
    }

    // ---------------------------------------------------------------------
    // Listado y tarjeta
    // ---------------------------------------------------------------------

    private fun actualizarPanel() {
        val enBusqueda = textoBusqueda.isNotEmpty()
        val visible = enBusqueda || categoria != null
        binding.panelLista.isVisible = visible
        if (!visible) return

        binding.tituloLista.text = when {
            enBusqueda -> getString(R.string.mapa_titulo_resultados, elementos.size)
            categoria == Categoria.VETERINARIAS -> getString(R.string.mapa_titulo_veterinarias, elementos.size)
            else -> getString(R.string.mapa_titulo_campanas, elementos.size)
        }
        // Ordenar y filtrar solo se ofrecen cuando se ve una categoría (no en los resultados de una búsqueda).
        val conControles = !enBusqueda && categoria != null
        binding.botonOrdenar.isVisible = conControles
        binding.botonFiltros.isVisible = conControles
        val cantidad = cantidadDeFiltros()
        binding.textoFiltros.text =
            if (cantidad > 0) getString(R.string.mapa_filtros_con_cantidad, cantidad) else getString(R.string.mapa_filtros)

        adaptador.actualizar(elementos, mostrarClase = enBusqueda)
        binding.mensajeVacio.isVisible = elementos.isEmpty()
        binding.lista.isVisible = elementos.isNotEmpty()
        if (elementos.isEmpty()) binding.mensajeVacio.setText(mensajeSinResultados())
    }

    @StringRes
    private fun mensajeSinResultados(): Int {
        if (textoBusqueda.isNotEmpty()) return R.string.mapa_sin_resultados_busqueda
        val soloDisponibleAhora = cantidadDeFiltros() == 1 && disponibleAhoraActivo()
        return when {
            soloDisponibleAhora -> R.string.mapa_sin_disponibles
            categoria == Categoria.VETERINARIAS -> R.string.mapa_sin_veterinarias
            else -> R.string.mapa_sin_campanas
        }
    }

    private fun cantidadDeFiltros() = when (categoria) {
        Categoria.VETERINARIAS -> filtrosVeterinarias.cantidad
        Categoria.CAMPANAS -> filtrosCampanas.cantidad
        null -> 0
    }

    private fun disponibleAhoraActivo() = when (categoria) {
        Categoria.VETERINARIAS -> filtrosVeterinarias.disponibleAhora
        Categoria.CAMPANAS -> filtrosCampanas.disponibleAhora
        null -> false
    }

    private fun alternarTamanoDelPanel() {
        panelExpandido = !panelExpandido
        val parametros = binding.lista.layoutParams
        parametros.height = dp(if (panelExpandido) 460 else 270)
        binding.lista.layoutParams = parametros
    }

    private fun seleccionar(elemento: ElementoMapa) {
        val contexto = requireContext()
        val lat = ubicacion?.latitude
        val lng = ubicacion?.longitude
        when (elemento) {
            // Una veterinaria abre su perfil (RF41).
            is ElementoMapa.Veterinaria -> startActivity(PerfilVeterinariaActivity.intent(contexto, elemento.id, lat, lng))
            // Una campaña de una veterinaria se ve dentro del perfil de esa veterinaria, desplegada; la de una entidad
            // externa abre su propia pantalla con el resumen de su organizador (RF43).
            is ElementoMapa.Campana -> {
                val veterinariaId = elemento.datos.veterinariaId
                startActivity(
                    if (veterinariaId != null) {
                        PerfilVeterinariaActivity.intent(contexto, veterinariaId, lat, lng, campanaId = elemento.id)
                    } else {
                        CampanaDetalleActivity.intent(contexto, elemento.id, lat, lng)
                    },
                )
            }
        }
    }

    // ---------------------------------------------------------------------
    // Textos de cada elemento
    // ---------------------------------------------------------------------

    private fun nombreDeEtiqueta(elemento: ElementoMapa): String = when (elemento) {
        is ElementoMapa.Veterinaria ->
            getString(if (elemento.datos.tipo == "municipal") R.string.mapa_tipo_municipal else R.string.mapa_tipo_privada)
        is ElementoMapa.Campana ->
            getString(if (elemento.datos.estado == "activa") R.string.mapa_estado_activa else R.string.mapa_estado_proxima)
    }

    private fun nombreDeClase(elemento: ElementoMapa) =
        getString(if (elemento is ElementoMapa.Veterinaria) R.string.mapa_etiqueta_veterinaria else R.string.mapa_etiqueta_campana)

    private fun textoDistancia(elemento: ElementoMapa) =
        elemento.distanciaKm?.let { "%.1f km".format(Locale.US, it) } ?: getString(R.string.mapa_sin_distancia)

    private fun textoCalificacion(promedio: Double?, total: Int) =
        if (promedio == null || total == 0) getString(R.string.mapa_sin_resenas) else "%.1f (%d)".format(Locale.US, promedio, total)

    /** "9:00" a partir de "09:00:00". */
    private fun hora(texto: String) = "${texto.take(2).toIntOrNull() ?: 0}:${texto.substring(3, 5)}"

    private fun textoHorarioHoy(franjas: List<FranjaHoy>): String {
        if (franjas.isEmpty()) return getString(R.string.mapa_hoy_cerrado)
        val rango = if (franjas.size == 1 && franjas[0].apertura.startsWith("00:00") && franjas[0].cierre >= "23:59") {
            getString(R.string.mapa_24_horas)
        } else {
            franjas.joinToString(", ") { "${hora(it.apertura)}–${hora(it.cierre)}" }
        }
        return getString(R.string.mapa_hoy_horario, rango)
    }

    /** "4 oct" a partir de "2026-10-04". */
    private fun fechaCorta(texto: String) = runCatching {
        LocalDate.parse(texto).format(DateTimeFormatter.ofPattern("d MMM", Locale("es", "PE"))).replace(".", "")
    }.getOrDefault(texto)

    private fun textoFechasCampana(c: CampanaRespuesta): String {
        val fechas = if (c.fechaInicio == c.fechaFin) fechaCorta(c.fechaInicio) else "${fechaCorta(c.fechaInicio)} – ${fechaCorta(c.fechaFin)}"
        val costo = getString(if (c.costo == "gratuita") R.string.mapa_costo_gratuita else R.string.mapa_costo_de_pago)
        return "$fechas · $costo"
    }

    private fun textoCampanasAqui(cantidad: Int) =
        if (cantidad == 1) getString(R.string.mapa_una_campana) else getString(R.string.mapa_varias_campanas, cantidad)

    // ---------------------------------------------------------------------
    // Orden
    // ---------------------------------------------------------------------

    private fun elegirOrden() {
        val esVeterinarias = categoria == Categoria.VETERINARIAS
        val opciones: List<Pair<String, Int>> = if (esVeterinarias) {
            listOf("distancia" to R.string.mapa_orden_distancia, "calificacion" to R.string.mapa_orden_calificacion)
        } else {
            listOf(
                "inicio" to R.string.mapa_orden_inicio,
                "fin" to R.string.mapa_orden_fin,
                "distancia" to R.string.mapa_orden_distancia,
                "calificacion" to R.string.mapa_orden_calificacion,
            )
        }
        val actual = (if (esVeterinarias) ordenVeterinarias ?: if (ubicacion != null) "distancia" else null else ordenCampanas ?: "inicio")
        val dialogo = BottomSheetDialog(requireContext())
        val hoja = HojaOrdenMapaBinding.inflate(layoutInflater)
        for ((clave, texto) in opciones) {
            val opcion = ItemOpcionOrdenBinding.inflate(layoutInflater, hoja.opciones, false)
            val elegida = clave == actual
            opcion.texto.setText(texto)
            opcion.root.setBackgroundResource(if (elegida) R.drawable.bg_opcion_orden_activa else R.drawable.bg_opcion_orden)
            opcion.circulo.setBackgroundResource(if (elegida) R.drawable.bg_radio_lleno else R.drawable.bg_radio_vacio)
            opcion.marca.isVisible = elegida
            opcion.root.setOnClickListener {
                dialogo.dismiss()
                aplicarOrden(clave)
            }
            hoja.opciones.addView(opcion.root)
        }
        mostrarHoja(dialogo, hoja.root)
    }

    private fun aplicarOrden(clave: String) {
        if (clave == "distancia" && ubicacion == null) {
            // Sin ubicación no hay distancia: se pide, y si no se logra se mantiene el orden anterior.
            pedirUbicacion { obtenida ->
                if (obtenida) fijarOrden(clave) else avisar(getString(R.string.mapa_activa_ubicacion))
            }
        } else {
            fijarOrden(clave)
        }
    }

    private fun fijarOrden(clave: String) {
        if (categoria == Categoria.VETERINARIAS) ordenVeterinarias = clave else ordenCampanas = clave
        recargar()
    }

    /** Muestra una hoja inferior abierta del todo y sin el fondo por defecto, para que se vean sus esquinas redondeadas. */
    private fun mostrarHoja(dialogo: BottomSheetDialog, contenido: View) {
        dialogo.setContentView(contenido)
        (contenido.parent as? View)?.setBackgroundColor(Color.TRANSPARENT)
        dialogo.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialogo.behavior.skipCollapsed = true
        dialogo.show()
    }

    // ---------------------------------------------------------------------
    // Filtros
    // ---------------------------------------------------------------------

    /** Lo que el usuario va eligiendo en la hoja de filtros, antes de aplicarlo. */
    private class Borrador(
        var ahora: Boolean = false,
        val tipos: MutableSet<String> = mutableSetOf(),
        val estados: MutableSet<String> = mutableSetOf(),
        val costos: MutableSet<String> = mutableSetOf(),
        val especies: MutableSet<String> = mutableSetOf(),
        val servicios: MutableSet<Int> = mutableSetOf(),
    )

    private fun abrirFiltros() {
        val actual = categoria ?: return
        val esVeterinarias = actual == Categoria.VETERINARIAS
        val borrador = if (esVeterinarias) {
            Borrador(filtrosVeterinarias.disponibleAhora, filtrosVeterinarias.tipos.toMutableSet(), especies = filtrosVeterinarias.especies.toMutableSet(), servicios = filtrosVeterinarias.servicios.toMutableSet())
        } else {
            Borrador(filtrosCampanas.disponibleAhora, estados = filtrosCampanas.estados.toMutableSet(), costos = filtrosCampanas.costos.toMutableSet(), especies = filtrosCampanas.especies.toMutableSet(), servicios = filtrosCampanas.servicios.toMutableSet())
        }

        val dialogo = BottomSheetDialog(requireContext())
        val hoja = HojaFiltrosMapaBinding.inflate(layoutInflater)
        var conteo: Job? = null
        var cantidadActual = elementos.size

        fun mostrarCantidad() {
            hoja.botonAplicar.text = if (cantidadActual == 1) getString(R.string.mapa_ver_resultado) else getString(R.string.mapa_ver_resultados, cantidadActual)
        }

        /** Cada cambio vuelve a contar cuántos resultados quedarían, con un pequeño retraso para no pedir de más. */
        fun recontar() {
            conteo?.cancel()
            conteo = viewLifecycleOwner.lifecycleScope.launch {
                delay(250)
                val lat = ubicacion?.latitude
                val lng = ubicacion?.longitude
                val total = if (esVeterinarias) {
                    repositorio.veterinarias(FiltrosVeterinarias(borrador.tipos, borrador.especies, borrador.servicios, borrador.ahora), null, lat, lng)
                        .let { if (it is Resultado.Exito) it.dato.size else null }
                } else {
                    repositorio.campanas(FiltrosCampanas(borrador.estados, borrador.especies, borrador.servicios, borrador.costos, borrador.ahora), null, lat, lng)
                        .let { if (it is Resultado.Exito) it.dato.size else null }
                }
                if (total != null) {
                    cantidadActual = total
                    mostrarCantidad()
                }
            }
        }

        hoja.textoAhora.setText(if (esVeterinarias) R.string.mapa_abierto_ahora else R.string.mapa_activa_abierta_ahora)
        hoja.interruptorAhora.isChecked = borrador.ahora
        hoja.interruptorAhora.setOnCheckedChangeListener { _, marcado ->
            borrador.ahora = marcado
            recontar()
        }

        val especies = (catalogos?.especies ?: listOf("perro", "gato", "conejo")).map { it to getString(nombrePlural(it)) }
        if (esVeterinarias) {
            agregarSeccion(hoja, R.string.mapa_filtro_tipo, listOf("privada" to getString(R.string.mapa_tipo_privada), "municipal" to getString(R.string.mapa_tipo_municipal)), borrador.tipos, ::recontar)
        } else {
            agregarSeccion(hoja, R.string.mapa_filtro_estado, listOf("activa" to getString(R.string.mapa_estado_activa), "proxima" to getString(R.string.mapa_estado_proxima)), borrador.estados, ::recontar)
            agregarSeccion(hoja, R.string.mapa_filtro_costo, listOf("gratuita" to getString(R.string.mapa_costo_gratuita), "de_pago" to getString(R.string.mapa_costo_de_pago)), borrador.costos, ::recontar)
        }
        agregarSeccion(hoja, R.string.mapa_filtro_especie, especies, borrador.especies, ::recontar)
        agregarServicios(hoja, borrador.servicios, ::recontar)

        mostrarCantidad()
        hoja.botonLimpiar.setOnClickListener {
            conteo?.cancel()
            if (esVeterinarias) filtrosVeterinarias = FiltrosVeterinarias() else filtrosCampanas = FiltrosCampanas()
            dialogo.dismiss()
            ajustarVista = true
            recargar()
        }
        hoja.botonAplicar.setOnClickListener {
            conteo?.cancel()
            if (esVeterinarias) {
                filtrosVeterinarias = FiltrosVeterinarias(borrador.tipos.toSet(), borrador.especies.toSet(), borrador.servicios.toSet(), borrador.ahora)
            } else {
                filtrosCampanas = FiltrosCampanas(borrador.estados.toSet(), borrador.especies.toSet(), borrador.servicios.toSet(), borrador.costos.toSet(), borrador.ahora)
            }
            dialogo.dismiss()
            ajustarVista = true
            recargar()
        }
        mostrarHoja(dialogo, hoja.root)
    }

    @StringRes
    private fun nombrePlural(especie: String) = when (especie) {
        "gato" -> R.string.mapa_especie_gato
        "conejo" -> R.string.mapa_especie_conejo
        else -> R.string.mapa_especie_perro
    }

    private fun encabezadoDeSeccion(@StringRes titulo: Int) = TextView(requireContext()).apply {
        setText(titulo)
        setTextColor(color(R.color.petcare_text_muted))
        textSize = 14f
        typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_semibold)
        setPadding(0, dp(16), 0, dp(8))
    }

    /** Un opción del filtro: se ve en blanco y, al elegirla, en verde azulado claro con borde. */
    private fun <T> crearOpcion(padre: ViewGroup, etiqueta: String, elegidas: MutableSet<T>, clave: T, alCambiar: () -> Unit): TextView {
        val opcion = layoutInflater.inflate(R.layout.item_chip_filtro, padre, false) as TextView
        opcion.text = etiqueta
        fun pintar() {
            val activa = clave in elegidas
            opcion.setBackgroundResource(if (activa) R.drawable.bg_chip_opcion_activo else R.drawable.bg_chip_opcion)
            opcion.setTextColor(color(if (activa) R.color.petcare_primary_dark else R.color.petcare_text))
        }
        pintar()
        opcion.setOnClickListener {
            if (clave in elegidas) elegidas.remove(clave) else elegidas.add(clave)
            pintar()
            alCambiar()
        }
        return opcion
    }

    private fun grupoDeOpciones() = ChipGroup(requireContext()).apply {
        chipSpacingHorizontal = dp(8)
        chipSpacingVertical = dp(8)
    }

    /** Un título y sus opciones en la hoja de filtros. */
    private fun <T> agregarSeccion(
        hoja: HojaFiltrosMapaBinding,
        @StringRes titulo: Int,
        opciones: List<Pair<T, String>>,
        elegidas: MutableSet<T>,
        alCambiar: () -> Unit,
    ) {
        if (opciones.isEmpty()) return
        val grupo = grupoDeOpciones()
        for ((clave, etiqueta) in opciones) grupo.addView(crearOpcion(grupo, etiqueta, elegidas, clave, alCambiar))
        hoja.secciones.addView(encabezadoDeSeccion(titulo))
        hoja.secciones.addView(grupo)
    }

    /** Los servicios van en una caja con sus tres categorías, que se abren y se cierran. */
    private fun agregarServicios(hoja: HojaFiltrosMapaBinding, elegidos: MutableSet<Int>, alCambiar: () -> Unit) {
        val servicios = catalogos?.servicios.orEmpty()
        if (servicios.isEmpty()) return
        val caja = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_tarjeta_borde)
            setPadding(dp(16), dp(2), dp(16), dp(2))
        }
        val categorias = servicios.groupBy { it.categoria }
        var primera = true
        for ((nombre, lista) in categorias) {
            if (!primera) caja.addView(View(requireContext()).apply {
                setBackgroundColor(color(R.color.petcare_border))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1))
            })
            primera = false

            val cabecera = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, dp(15), 0, dp(15))
                isClickable = true
            }
            val titulo = TextView(requireContext()).apply {
                text = nombre
                setTextColor(color(R.color.petcare_text))
                textSize = 15f
                typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_semibold)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            val contador = TextView(requireContext()).apply {
                setTextColor(color(R.color.white))
                textSize = 12f
                gravity = android.view.Gravity.CENTER
                typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_semibold)
                setBackgroundResource(R.drawable.bg_radio_lleno)
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply { marginEnd = dp(8) }
            }
            val flecha = ImageView(requireContext()).apply {
                setImageResource(R.drawable.ic_desplegar)
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22))
            }
            cabecera.addView(titulo)
            cabecera.addView(contador)
            cabecera.addView(flecha)

            val grupo = grupoDeOpciones().apply {
                visibility = View.GONE
                setPadding(0, 0, 0, dp(14))
            }
            fun actualizarContador() {
                val cantidad = lista.count { it.id in elegidos }
                contador.isVisible = cantidad > 0
                contador.text = cantidad.toString()
            }
            for (servicio in lista) {
                grupo.addView(crearOpcion(grupo, servicio.nombre, elegidos, servicio.id) {
                    actualizarContador()
                    alCambiar()
                })
            }
            actualizarContador()
            cabecera.setOnClickListener {
                val abrir = !grupo.isVisible
                grupo.isVisible = abrir
                flecha.rotation = if (abrir) 180f else 0f
            }
            caja.addView(cabecera)
            caja.addView(grupo)
        }
        hoja.secciones.addView(encabezadoDeSeccion(R.string.mapa_filtro_servicio))
        hoja.secciones.addView(caja)
    }

    // ---------------------------------------------------------------------
    // Ubicación del usuario
    // ---------------------------------------------------------------------

    /** Botón de la mira: vuelve a obtener la ubicación y centra el mapa en ella, esté donde esté el mapa. */
    private fun irAMiUbicacion() {
        pedirUbicacion { obtenida ->
            val punto = ubicacion
            if (obtenida && punto != null) {
                binding.mapa.controller.animateTo(punto, 16.0, 600L)
                recargar() // con la posición nueva, las distancias del listado se actualizan
            } else {
                avisar(getString(R.string.mapa_ubicacion_no_disponible))
            }
        }
    }

    private fun pedirUbicacion(alTerminar: (Boolean) -> Unit) {
        if (UbicacionActual.tienePermiso(requireContext())) {
            obtenerUbicacion(alTerminar)
        } else {
            alTenerPermiso = alTerminar
            pedirPermiso.launch(UbicacionActual.PERMISOS)
        }
    }

    private fun obtenerUbicacion(alTerminar: (Boolean) -> Unit) {
        UbicacionActual.obtener(requireContext()) { lugar ->
            if (_binding == null) return@obtener
            if (lugar == null) {
                alTerminar(false)
            } else {
                ubicacion = GeoPoint(lugar.latitude, lugar.longitude)
                mostrarMiUbicacion()
                alTerminar(true)
            }
        }
    }

    // ---------------------------------------------------------------------
    // Utilidades y ciclo de vida
    // ---------------------------------------------------------------------

    private fun avisar(mensaje: String) {
        context?.let { Toast.makeText(it, mensaje, Toast.LENGTH_LONG).show() }
    }

    private fun dp(valor: Int) = (valor * resources.displayMetrics.density).toInt()

    private fun color(@ColorRes id: Int) = ContextCompat.getColor(requireContext(), id)

    private fun ocultarTeclado() {
        val gestor = requireContext().getSystemService(InputMethodManager::class.java)
        gestor.hideSoftInputFromWindow(binding.campoBusqueda.windowToken, 0)
        binding.campoBusqueda.clearFocus()
    }

    override fun onResume() {
        super.onResume()
        _binding?.mapa?.onResume()
        // Si el usuario guardó, editó o eliminó una reseña, las calificaciones se actualizan al volver al mapa.
        if (ResenasCambiaron.consumir() && _binding != null) recargar()
    }

    override fun onPause() {
        _binding?.mapa?.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        trabajo?.cancel()
        binding.mapa.onDetach()
        marcadores.clear()
        marcadorUbicacion = null
        _binding = null
        super.onDestroyView()
    }

    // ---------------------------------------------------------------------
    // Listado
    // ---------------------------------------------------------------------

    private inner class ResultadosAdapter : RecyclerView.Adapter<ResultadosAdapter.Fila>() {
        private var datos: List<ElementoMapa> = emptyList()
        private var mostrarClase = false

        @SuppressLint("NotifyDataSetChanged")
        fun actualizar(nuevos: List<ElementoMapa>, mostrarClase: Boolean) {
            datos = nuevos
            this.mostrarClase = mostrarClase
            notifyDataSetChanged()
        }

        inner class Fila(val vista: ItemResultadoMapaBinding) : RecyclerView.ViewHolder(vista.root)

        override fun onCreateViewHolder(padre: ViewGroup, tipo: Int) =
            Fila(ItemResultadoMapaBinding.inflate(LayoutInflater.from(padre.context), padre, false))

        override fun getItemCount() = datos.size

        override fun onBindViewHolder(fila: Fila, posicion: Int) {
            val elemento = datos[posicion]
            val v = fila.vista
            val esVeterinaria = elemento is ElementoMapa.Veterinaria

            // Ícono de color, igual al del marcador
            val colorIcono = when (elemento) {
                is ElementoMapa.Veterinaria -> MarcadoresMapa.colorDeVeterinaria(elemento.datos.tipo)
                is ElementoMapa.Campana -> MarcadoresMapa.colorDeCampana(elemento.datos.estado)
            }
            v.icono.setImageResource(if (esVeterinaria) R.drawable.ic_mapa_veterinaria else R.drawable.ic_mapa_campana)
            v.icono.backgroundTintList = ColorStateList.valueOf(color(colorIcono))

            v.nombre.text = elemento.nombre
            v.etiquetaClase.isVisible = mostrarClase
            v.etiquetaClase.text = nombreDeClase(elemento)
            v.etiqueta.text = nombreDeEtiqueta(elemento)
            val (fondo, texto) = coloresDeEtiqueta(elemento)
            v.etiqueta.backgroundTintList = ColorStateList.valueOf(color(fondo))
            v.etiqueta.setTextColor(color(texto))

            when (elemento) {
                is ElementoMapa.Veterinaria -> {
                    val d = elemento.datos
                    v.detalle.text = "${textoDistancia(elemento)} · ${textoHorarioHoy(d.horarioHoy)}"
                    v.detalle2.isVisible = false
                    v.filaEstrellas.isVisible = true
                    pintarEstrellas(v.estrellas, d.promedio)
                    v.textoCalificacion.text = textoCalificacion(d.promedio, d.totalResenas)
                    v.filaCampanas.isVisible = d.campanasAqui.isNotEmpty()
                    v.textoCampanas.text = textoCampanasAqui(d.campanasAqui.size)
                }
                is ElementoMapa.Campana -> {
                    val c = elemento.datos
                    v.detalle.text = "${c.organizador} · ${textoDistancia(elemento)}"
                    v.detalle2.isVisible = true
                    v.detalle2.text = textoFechasCampana(c)
                    // Calificación del organizador (RF35): es el criterio de "Ordenar por calificación".
                    v.filaEstrellas.isVisible = true
                    pintarEstrellas(v.estrellas, c.promedio)
                    v.textoCalificacion.text = textoCalificacion(c.promedio, c.totalResenas)
                    v.filaCampanas.isVisible = false
                }
            }
            v.root.setOnClickListener { seleccionar(elemento) }
        }

        private fun coloresDeEtiqueta(elemento: ElementoMapa): Pair<Int, Int> = when (elemento) {
            is ElementoMapa.Veterinaria ->
                if (elemento.datos.tipo == "municipal") R.color.etiqueta_municipal_fondo to R.color.etiqueta_municipal_texto
                else R.color.etiqueta_privada_fondo to R.color.etiqueta_privada_texto
            is ElementoMapa.Campana ->
                if (elemento.datos.estado == "activa") R.color.etiqueta_activa_fondo to R.color.etiqueta_activa_texto
                else R.color.etiqueta_proxima_fondo to R.color.etiqueta_proxima_texto
        }

        /** Cinco estrellas: llenas hasta la calificación redondeada (como en el prototipo) y vacías el resto. */
        private fun pintarEstrellas(contenedor: LinearLayout, promedio: Double?) {
            contenedor.removeAllViews()
            val llenas = promedio?.let { Math.round(it).toInt() } ?: 0
            for (i in 1..5) {
                contenedor.addView(
                    ImageView(contenedor.context).apply {
                        setImageResource(if (i <= llenas) R.drawable.ic_estrella_llena else R.drawable.ic_estrella_vacia)
                        layoutParams = LinearLayout.LayoutParams(dp(15), dp(15))
                    },
                )
            }
        }
    }
}
