package com.petcare.app.cercania

import android.annotation.SuppressLint
import android.graphics.DashPathEffect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.petcare.app.R
import com.petcare.app.common.Resultado
import com.petcare.app.common.UbicacionActual
import com.petcare.app.common.paraMostrar
import com.petcare.app.common.respetarBarrasYTeclado
import com.petcare.app.databinding.ActivityCercaniaBinding
import com.petcare.app.mapa.MarcadoresMapa
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.io.File

/**
 * Avisos cerca de ti (RF37 y RF38): el usuario elige su ubicación de referencia (buscando una dirección,
 * tocando el mapa, arrastrando el marcador o usando su ubicación actual), un radio de 1 a 10 km y qué avisos
 * quiere recibir. El servidor revisa cada pocos minutos si hay campañas o veterinarias nuevas dentro del radio.
 */
class CercaniaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCercaniaBinding
    private val repositorio = CercaniaRepository()

    private var punto: GeoPoint? = null
    private var marcador: Marker? = null
    private var circulo: Polygon? = null
    private var busqueda: Job? = null
    private var alEscribir = true // false mientras el código escribe en el campo, para no volver a buscar
    private var alTenerPermiso: (() -> Unit)? = null

    private val pedirPermiso =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permisos ->
            val pendiente = alTenerPermiso
            alTenerPermiso = null
            if (permisos.values.any { it }) pendiente?.invoke() else mostrarError(getString(R.string.cercania_permiso_ubicacion))
        }

    /** Radio elegido en kilómetros: la barra va de 0 a 9 y representa de 1 a 10. */
    private val radioKm get() = binding.barraDistancia.progress + 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCercaniaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.raiz.respetarBarrasYTeclado()

        prepararMapa()
        prepararControles()
        actualizarEstado()
        cargar()
    }

    // ---------------------------------------------------------------------
    // Preparación
    // ---------------------------------------------------------------------

    @SuppressLint("ClickableViewAccessibility")
    private fun prepararMapa() {
        val aplicacion = applicationContext
        Configuration.getInstance().apply {
            load(aplicacion, aplicacion.getSharedPreferences("osmdroid", MODE_PRIVATE))
            userAgentValue = aplicacion.packageName
            osmdroidBasePath = File(aplicacion.cacheDir, "osmdroid")
            osmdroidTileCache = File(osmdroidBasePath, "tiles")
        }
        binding.mapa.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(13.0)
            controller.setCenter(DireccionesApi.LIMA)
            // OpenStreetMap exige mostrar su atribución (© colaboradores de OpenStreetMap) sobre el mapa.
            overlays.add(CopyrightOverlay(this@CercaniaActivity))
            // Tocar un lugar del mapa lo elige como ubicación de referencia.
            overlays.add(
                MapEventsOverlay(
                    object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                            p?.let { elegirPunto(it, buscarDireccion = true) }
                            return true
                        }

                        override fun longPressHelper(p: GeoPoint?) = false
                    },
                ),
            )
            // El mapa está dentro de una pantalla que se desliza: mientras el dedo está sobre él, el mapa manda.
            setOnTouchListener { vista, evento ->
                when (evento.actionMasked) {
                    MotionEvent.ACTION_DOWN -> vista.parent.requestDisallowInterceptTouchEvent(true)
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> vista.parent.requestDisallowInterceptTouchEvent(false)
                }
                false
            }
        }
    }

    private fun prepararControles() {
        binding.botonVolver.setOnClickListener { finish() }
        binding.botonGuardar.setOnClickListener { guardar() }
        binding.botonUbicacionActual.setOnClickListener { usarMiUbicacion() }

        binding.barraDistancia.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(barra: SeekBar?, progreso: Int, delUsuario: Boolean) {
                    actualizarRadio(ajustarMapa = delUsuario)
                }

                override fun onStartTrackingTouch(barra: SeekBar?) = Unit
                override fun onStopTrackingTouch(barra: SeekBar?) = Unit
            },
        )

        // RF38: sin ubicación y radio no se puede activar ningún aviso.
        binding.interruptorCampanas.setOnCheckedChangeListener { vista, marcado ->
            if (marcado && punto == null) {
                vista.isChecked = false
                mostrarError(getString(R.string.cercania_sin_ubicacion))
            } else {
                ocultarError()
            }
        }
        binding.interruptorVeterinarias.setOnCheckedChangeListener { vista, marcado ->
            if (marcado && punto == null) {
                vista.isChecked = false
                mostrarError(getString(R.string.cercania_sin_ubicacion))
            } else {
                ocultarError()
            }
        }

        binding.campoDireccion.doAfterTextChanged { texto ->
            if (alEscribir) sugerir(texto?.toString().orEmpty().trim())
        }
        binding.campoDireccion.setOnEditorActionListener { _, accion, _ ->
            if (accion == EditorInfo.IME_ACTION_SEARCH) {
                ocultarTeclado()
                true
            } else {
                false
            }
        }
    }

    private fun cargar() {
        lifecycleScope.launch {
            when (val resultado = repositorio.obtener()) {
                is Resultado.Exito -> {
                    val c = resultado.dato
                    c.radioKm?.let { binding.barraDistancia.progress = (Math.round(it).toInt() - 1).coerceIn(0, 9) }
                    if (c.lat != null && c.lng != null) elegirPunto(GeoPoint(c.lat, c.lng), buscarDireccion = true)
                    binding.interruptorCampanas.isChecked = c.notificarCampanas
                    binding.interruptorVeterinarias.isChecked = c.notificarVeterinarias
                    actualizarEstado()
                }
                is Resultado.Fallo -> mostrarError(resultado.paraMostrar(this@CercaniaActivity))
            }
        }
    }

    // ---------------------------------------------------------------------
    // Ubicación de referencia
    // ---------------------------------------------------------------------

    /** Coloca (o mueve) el marcador en [nuevo] y dibuja el círculo del radio. */
    private fun elegirPunto(nuevo: GeoPoint, buscarDireccion: Boolean) {
        punto = nuevo
        ocultarError()
        val mapa = binding.mapa
        val actual = marcador ?: Marker(mapa).also { m ->
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            m.icon = MarcadoresMapa.referencia(this)
            m.isDraggable = true
            m.setOnMarkerClickListener { _, _ -> true }
            m.setOnMarkerDragListener(
                object : Marker.OnMarkerDragListener {
                    override fun onMarkerDrag(marker: Marker?) {
                        marker?.let { punto = it.position }
                        dibujarCirculo()
                    }

                    override fun onMarkerDragEnd(marker: Marker?) {
                        marker?.let { elegirPunto(it.position, buscarDireccion = true) }
                    }

                    override fun onMarkerDragStart(marker: Marker?) = Unit
                },
            )
            mapa.overlays.add(m)
            marcador = m
        }
        actual.position = nuevo
        dibujarCirculo()
        ajustarMapa()
        actualizarEstado()
        if (buscarDireccion) escribirDireccionDe(nuevo)
    }

    private fun dibujarCirculo() {
        val centro = punto ?: return
        val mapa = binding.mapa
        val verde = ContextCompat.getColor(this, R.color.petcare_primary)
        val circulo = circulo ?: Polygon(mapa).also { p ->
            p.fillPaint.color = ColorUtils.setAlphaComponent(verde, 40)
            p.outlinePaint.color = verde
            p.outlinePaint.strokeWidth = 4f
            p.outlinePaint.pathEffect = DashPathEffect(floatArrayOf(18f, 12f), 0f) // borde punteado, como en el diseño
            p.setOnClickListener { _, _, _ -> false }
            mapa.overlays.add(0, p) // debajo del marcador
            this.circulo = p
        }
        circulo.points = Polygon.pointsAsCircle(centro, radioKm * 1000.0)
        mapa.invalidate()
    }

    /** Ajusta el zoom para que se vea el círculo completo. */
    private fun ajustarMapa() {
        val centro = punto ?: return
        val mapa = binding.mapa
        val puntos = Polygon.pointsAsCircle(centro, radioKm * 1000.0 * 1.25)
        val caja = BoundingBox.fromGeoPoints(puntos)
        mapa.post { binding.mapa.zoomToBoundingBox(caja, false, 8) }
    }

    private fun actualizarRadio(ajustarMapa: Boolean) {
        binding.textoDistancia.text = getString(R.string.cercania_distancia_valor, radioKm)
        dibujarCirculo()
        if (ajustarMapa) ajustarMapa()
    }

    /** Lo que se ve depende de si ya hay una ubicación elegida: la indicación del mapa y la barra del radio. */
    private fun actualizarEstado() {
        val configurado = punto != null
        binding.indicacionMapa.isVisible = !configurado
        binding.barraDistancia.isEnabled = configurado
        binding.barraDistancia.alpha = if (configurado) 1f else 0.45f
        binding.textoDistancia.alpha = if (configurado) 1f else 0.45f
        binding.textoDistancia.text = getString(R.string.cercania_distancia_valor, radioKm)
    }

    private fun usarMiUbicacion() {
        ocultarError()
        if (!UbicacionActual.tienePermiso(this)) {
            alTenerPermiso = { usarMiUbicacion() }
            pedirPermiso.launch(UbicacionActual.PERMISOS)
            return
        }
        UbicacionActual.obtener(this) { lugar ->
            if (lugar == null) {
                mostrarError(getString(R.string.cercania_ubicacion_no_disponible))
            } else {
                elegirPunto(GeoPoint(lugar.latitude, lugar.longitude), buscarDireccion = false)
                escribirEnElCampo(getString(R.string.cercania_mi_ubicacion))
            }
        }
    }

    // ---------------------------------------------------------------------
    // Buscador de direcciones
    // ---------------------------------------------------------------------

    /** Mientras el usuario escribe, ofrece direcciones. Espera un momento para no consultar con cada letra. */
    private fun sugerir(texto: String) {
        busqueda?.cancel()
        if (texto.length < 3) {
            ocultarSugerencias()
            return
        }
        busqueda = lifecycleScope.launch {
            delay(400)
            mostrarSugerencias(Direcciones.buscar(texto))
        }
    }

    private fun mostrarSugerencias(direcciones: List<Direccion>) {
        val lista = binding.sugerencias
        lista.removeAllViews()
        if (direcciones.isEmpty()) {
            lista.visibility = View.GONE
            return
        }
        for (direccion in direcciones) {
            val fila = TextView(this).apply {
                text = direccion.texto
                textSize = 14f
                setTextColor(ContextCompat.getColor(context, R.color.petcare_text))
                typeface = ResourcesCompat.getFont(context, R.font.poppins_regular)
                maxLines = 2
                setPadding(dp(16), dp(12), dp(16), dp(12))
                isClickable = true
                setBackgroundResource(android.R.drawable.list_selector_background)
                setOnClickListener {
                    ocultarTeclado()
                    ocultarSugerencias()
                    escribirEnElCampo(direccion.texto)
                    elegirPunto(direccion.punto, buscarDireccion = false)
                }
            }
            lista.addView(fila)
        }
        lista.visibility = View.VISIBLE
    }

    private fun ocultarSugerencias() {
        binding.sugerencias.removeAllViews()
        binding.sugerencias.visibility = View.GONE
    }

    /** Escribe en el campo sin que eso dispare una nueva búsqueda. */
    private fun escribirEnElCampo(texto: String) {
        alEscribir = false
        binding.campoDireccion.setText(texto)
        alEscribir = true
    }

    /** Al tocar el mapa o arrastrar el marcador, se averigua la dirección de ese punto y se muestra en el campo. */
    private fun escribirDireccionDe(lugar: GeoPoint) {
        lifecycleScope.launch {
            val direccion = Direcciones.deUnPunto(lugar)
            // Si mientras tanto el usuario movió el marcador a otro lado, esta respuesta ya no corresponde.
            if (punto != lugar) return@launch
            ocultarSugerencias()
            escribirEnElCampo(direccion?.texto ?: getString(R.string.cercania_ubicacion_elegida))
        }
    }

    // ---------------------------------------------------------------------
    // Guardar
    // ---------------------------------------------------------------------

    private fun guardar() {
        ocultarError()
        val centro = punto
        val configuracion = ConfiguracionCercania(
            lat = centro?.latitude,
            lng = centro?.longitude,
            radioKm = if (centro != null) radioKm.toDouble() else null,
            notificarCampanas = binding.interruptorCampanas.isChecked,
            notificarVeterinarias = binding.interruptorVeterinarias.isChecked,
        )
        binding.botonGuardar.isEnabled = false
        lifecycleScope.launch {
            when (val resultado = repositorio.guardar(configuracion)) {
                is Resultado.Exito -> {
                    Toast.makeText(this@CercaniaActivity, R.string.cercania_guardada, Toast.LENGTH_SHORT).show()
                    finish()
                }
                is Resultado.Fallo -> {
                    mostrarError(resultado.paraMostrar(this@CercaniaActivity))
                    binding.botonGuardar.isEnabled = true
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Utilidades y ciclo de vida
    // ---------------------------------------------------------------------

    private fun mostrarError(mensaje: String) {
        binding.textoError.text = mensaje
        binding.textoError.visibility = View.VISIBLE
        binding.desplazamiento.post { binding.desplazamiento.fullScroll(View.FOCUS_DOWN) }
    }

    private fun ocultarError() {
        binding.textoError.visibility = View.GONE
    }

    private fun dp(valor: Int) = (valor * resources.displayMetrics.density).toInt()

    private fun ocultarTeclado() {
        val gestor = getSystemService(InputMethodManager::class.java)
        gestor.hideSoftInputFromWindow(binding.campoDireccion.windowToken, 0)
        binding.campoDireccion.clearFocus()
    }

    override fun onResume() {
        super.onResume()
        binding.mapa.onResume()
    }

    override fun onPause() {
        binding.mapa.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        binding.mapa.onDetach()
        super.onDestroy()
    }
}
