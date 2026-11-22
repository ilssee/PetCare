package com.petcare.app.mapa

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import coil.load
import com.petcare.app.R
import com.petcare.app.common.Enlaces
import com.petcare.app.common.CuadriculaDeOpciones
import com.petcare.app.common.Resultado
import com.petcare.app.common.paraMostrar
import com.petcare.app.common.respetarBarrasYTeclado
import com.petcare.app.databinding.ActivityPerfilVeterinariaBinding
import com.petcare.app.mascota.Especies
import com.petcare.app.mascota.comoUrlFoto
import com.petcare.app.resena.EscribirResenaActivity
import com.petcare.app.resena.ResenaFila
import com.petcare.app.resena.ResenasActivity
import kotlinx.coroutines.launch
import java.net.URI

/**
 * Perfil de una veterinaria (RF41), con las pestañas Servicios, Contacto, Reseñas y Campañas. Se abre al tocar
 * una veterinaria en el mapa o en el listado. Cada campaña de la veterinaria se despliega con su detalle (RF43).
 */
class PerfilVeterinariaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerfilVeterinariaBinding
    private val repositorio = MapaRepository()

    private var perfil: PerfilVeterinariaRespuesta? = null
    private var pestana = Pestana.SERVICIOS
    private var fotoActual = 0

    // Campañas desplegadas. Si se llegó desde una campaña (RF43), se abre en la pestaña Campañas con esa campaña a la vista.
    private val campanasAbiertas = mutableSetOf<Int>()
    private var campanaResaltada = 0
    private var tarjetaResaltada: View? = null

    // Filtro de campañas por estado (RF42).
    private var filtroActivas = false
    private var filtroProximas = false

    private enum class Pestana { SERVICIOS, CONTACTO, RESENAS, CAMPANAS }

    private val organizadorId get() = intent.getIntExtra(EXTRA_ID, 0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPerfilVeterinariaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.raiz.respetarBarrasYTeclado()

        binding.botonVolver.setOnClickListener { finish() }
        binding.pestanaServicios.setOnClickListener { elegirPestana(Pestana.SERVICIOS) }
        binding.pestanaContacto.setOnClickListener { elegirPestana(Pestana.CONTACTO) }
        binding.pestanaResenas.setOnClickListener { elegirPestana(Pestana.RESENAS) }
        binding.pestanaCampanas.setOnClickListener { elegirPestana(Pestana.CAMPANAS) }
        binding.fotoAnterior.setOnClickListener { cambiarFoto(-1) }
        binding.fotoSiguiente.setOnClickListener { cambiarFoto(1) }

        if (savedInstanceState == null) {
            campanaResaltada = intent.getIntExtra(EXTRA_CAMPANA, 0)
            if (campanaResaltada != 0) {
                campanasAbiertas.add(campanaResaltada)
                pestana = Pestana.CAMPANAS
            }
        }
    }

    // Al volver de escribir, editar o eliminar una reseña, el perfil se actualiza solo.
    override fun onResume() {
        super.onResume()
        cargar()
    }

    private fun cargar() {
        val lat = if (intent.hasExtra(EXTRA_LAT)) intent.getDoubleExtra(EXTRA_LAT, 0.0) else null
        val lng = if (intent.hasExtra(EXTRA_LNG)) intent.getDoubleExtra(EXTRA_LNG, 0.0) else null
        lifecycleScope.launch {
            when (val resultado = repositorio.perfilDeVeterinaria(organizadorId, lat, lng)) {
                is Resultado.Exito -> {
                    perfil = resultado.dato
                    binding.mensajeError.isVisible = false
                    pintar(resultado.dato)
                }
                is Resultado.Fallo -> {
                    if (perfil == null) {
                        binding.desplazamiento.isVisible = false
                        binding.mensajeError.isVisible = true
                        binding.mensajeError.text = resultado.paraMostrar(this@PerfilVeterinariaActivity)
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Cabecera
    // ---------------------------------------------------------------------

    private fun pintar(p: PerfilVeterinariaRespuesta) {
        binding.contenido.visibility = View.VISIBLE
        binding.nombre.text = p.nombre
        val tieneDireccion = !p.direccion.isNullOrBlank()
        binding.iconoDireccion.isVisible = tieneDireccion
        binding.direccion.isVisible = tieneDireccion
        binding.espacioDireccion.isVisible = !tieneDireccion
        binding.direccion.text = p.direccion.orEmpty()
        binding.comoLlegar.setOnClickListener { Enlaces.comoLlegar(this, p.lat, p.lng) }

        val municipal = p.tipo == "municipal"
        binding.etiquetaTipo.setText(if (municipal) R.string.mapa_tipo_municipal else R.string.mapa_tipo_privada)
        binding.etiquetaTipo.backgroundTintList =
            ColorStateList.valueOf(color(if (municipal) R.color.etiqueta_municipal_fondo else R.color.etiqueta_privada_fondo))
        binding.etiquetaTipo.setTextColor(color(if (municipal) R.color.etiqueta_municipal_texto else R.color.etiqueta_privada_texto))
        binding.ilustracion.backgroundTintList = ColorStateList.valueOf(color(MarcadoresMapa.colorDeVeterinaria(p.tipo)))

        binding.calificacion.text = p.promedio?.let { FormatosMapa.calificacion(it) } ?: getString(R.string.mapa_sin_resenas)
        binding.distancia.isVisible = p.distanciaKm != null
        binding.distancia.text = p.distanciaKm?.let { "· ${"%.1f".format(java.util.Locale.US, it)} km" }.orEmpty()

        binding.estado.setText(if (p.abiertaAhora) R.string.perfil_abierto else R.string.perfil_cerrado)
        binding.estado.setTextColor(color(if (p.abiertaAhora) R.color.mapa_activa else R.color.petcare_error))
        binding.horarioHoy.text = "· " + FormatosMapa.horarioDeHoy(this, p.horarioHoy)

        pintarFotos(p)
        // Una veterinaria sin campañas no muestra esa pestaña (RF41).
        binding.pestanaCampanas.isVisible = p.campanas.isNotEmpty()
        if (pestana == Pestana.CAMPANAS && p.campanas.isEmpty()) pestana = Pestana.SERVICIOS

        pintarServicios(p)
        pintarContacto(p)
        pintarResenas(p)
        tarjetaResaltada = null
        pintarCampanas(p)
        elegirPestana(pestana)

        // Solo la primera vez: lleva el desplazamiento hasta la campaña elegida.
        val destino = tarjetaResaltada
        if (destino != null && campanaResaltada != 0) {
            campanaResaltada = 0
            binding.desplazamiento.post {
                binding.desplazamiento.smoothScrollTo(0, distanciaDesde(binding.contenido, destino))
            }
        }
    }

    // ---------------------------------------------------------------------
    // Fotos
    // ---------------------------------------------------------------------

    /** Sin fotos se ve la ilustración de la veterinaria; con fotos, un carrusel con contador y puntos. */
    private fun pintarFotos(p: PerfilVeterinariaRespuesta) {
        val hayFotos = p.fotos.isNotEmpty()
        binding.foto.isVisible = hayFotos
        binding.ilustracion.isVisible = !hayFotos
        val variasFotos = p.fotos.size > 1
        binding.contadorFotos.isVisible = variasFotos
        binding.fotoAnterior.isVisible = variasFotos
        binding.fotoSiguiente.isVisible = variasFotos
        binding.puntosFotos.isVisible = variasFotos
        fotoActual = fotoActual.coerceIn(0, maxOf(p.fotos.size - 1, 0))
        if (hayFotos) mostrarFoto(p)
    }

    private fun cambiarFoto(paso: Int) {
        val p = perfil ?: return
        if (p.fotos.size < 2) return
        fotoActual = (fotoActual + paso + p.fotos.size) % p.fotos.size
        mostrarFoto(p)
    }

    private fun mostrarFoto(p: PerfilVeterinariaRespuesta) {
        binding.foto.load(p.fotos[fotoActual].comoUrlFoto())
        binding.contadorFotos.text = getString(R.string.perfil_foto_contador, fotoActual + 1, p.fotos.size)
        binding.puntosFotos.removeAllViews()
        for (i in p.fotos.indices) {
            val punto = View(this).apply {
                setBackgroundResource(R.drawable.bg_punto_foto)
                backgroundTintList = ColorStateList.valueOf(color(if (i == fotoActual) R.color.petcare_primary else R.color.white))
                layoutParams = LinearLayout.LayoutParams(dp(7), dp(7)).apply { marginStart = if (i == 0) 0 else dp(5) }
            }
            binding.puntosFotos.addView(punto)
        }
    }

    // ---------------------------------------------------------------------
    // Pestañas
    // ---------------------------------------------------------------------

    private fun elegirPestana(nueva: Pestana) {
        pestana = nueva
        val titulos = mapOf(
            Pestana.SERVICIOS to binding.pestanaServicios,
            Pestana.CONTACTO to binding.pestanaContacto,
            Pestana.RESENAS to binding.pestanaResenas,
            Pestana.CAMPANAS to binding.pestanaCampanas,
        )
        for ((p, vista) in titulos) {
            val activa = p == nueva
            vista.setBackgroundResource(if (activa) R.drawable.bg_pestana_activa else 0)
            vista.setTextColor(color(if (activa) R.color.white else R.color.petcare_text_muted))
        }
        binding.contenidoServicios.isVisible = nueva == Pestana.SERVICIOS
        binding.contenidoContacto.isVisible = nueva == Pestana.CONTACTO
        binding.contenidoResenas.isVisible = nueva == Pestana.RESENAS
        binding.contenidoCampanas.isVisible = nueva == Pestana.CAMPANAS
    }

    // ---------------------------------------------------------------------
    // Pestaña Servicios
    // ---------------------------------------------------------------------

    private fun pintarServicios(p: PerfilVeterinariaRespuesta) {
        val cont = binding.contenidoServicios
        cont.removeAllViews()

        if (p.especies.isNotEmpty()) {
            cont.addView(titulo(R.string.perfil_especies_titulo))
            val fila = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            // Solo las especies que atiende, repartiéndose todo el ancho: una sola ocupa todo, dos la mitad cada una.
            val atendidas = listOf("perro", "gato", "conejo").filter { it in p.especies }
            atendidas.forEachIndexed { posicion, especie -> fila.addView(tarjetaDeEspecie(especie, primera = posicion == 0)) }
            cont.addView(fila)
        }

        cont.addView(titulo(R.string.perfil_servicios_titulo))
        if (p.serviciosPorCategoria.isEmpty()) {
            cont.addView(textoSimple(getString(R.string.perfil_sin_servicios)))
        }
        p.serviciosPorCategoria.forEach { categoria ->
            cont.addView(tarjetaDeCategoria(categoria))
        }
    }

    private fun tarjetaDeEspecie(especie: String, primera: Boolean): View {
        val tarjeta = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_icono_resultado)
            backgroundTintList = ColorStateList.valueOf(color(Especies.color(especie)))
            // El espacio entre tarjetas va antes de cada una menos la primera, para que la última llegue al borde.
            layoutParams = LinearLayout.LayoutParams(0, dp(100), 1f).apply { marginStart = if (primera) 0 else dp(8) }
        }
        tarjeta.addView(
            ImageView(this).apply {
                setImageResource(Especies.ilustracion(especie))
                layoutParams = LinearLayout.LayoutParams(dp(46), dp(46)).apply { gravity = Gravity.CENTER_HORIZONTAL }
            },
        )
        tarjeta.addView(
            TextView(this).apply {
                // Ocupa todo el ancho de la tarjeta y centra su texto, así el nombre queda justo debajo de la ilustración.
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                gravity = Gravity.CENTER_HORIZONTAL
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setText(
                    when (especie) {
                        "gato" -> R.string.mapa_especie_gato
                        "conejo" -> R.string.mapa_especie_conejo
                        else -> R.string.mapa_especie_perro
                    },
                )
                setTextColor(color(R.color.petcare_text))
                textSize = 14f
                typeface = fuente(R.font.poppins_semibold)
                setPadding(0, dp(6), 0, 0)
            },
        )
        return tarjeta
    }

    /** El ícono depende del nombre de la categoría, no de su posición: así no cambia si la veterinaria no ofrece alguna. */
    @DrawableRes
    private fun iconoDeCategoria(nombre: String): Int {
        val texto = nombre.lowercase()
        return when {
            "higiene" in texto || "bienestar" in texto -> R.drawable.ic_regalo
            "especialidad" in texto -> R.drawable.ic_corazon
            else -> R.drawable.ic_escudo // medicina preventiva y cualquier otra
        }
    }

    private fun tarjetaDeCategoria(categoria: ServiciosDeCategoria): View {
        val icono = iconoDeCategoria(categoria.categoria)
        val tarjeta = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_tarjeta_borde)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { bottomMargin = dp(12) }
        }
        val cantidad = categoria.servicios.size
        val encabezado = filaConIcono(
            icono,
            categoria.categoria,
            if (cantidad == 1) getString(R.string.perfil_un_servicio) else getString(R.string.perfil_varios_servicios, cantidad),
        )
        tarjeta.addView(encabezado)

        // Todos los servicios, en dos columnas iguales; si queda uno solo al final, ocupa toda la fila.
        val chips = categoria.servicios.map { servicio ->
            TextView(this).apply {
                text = servicio
                gravity = Gravity.CENTER
                setBackgroundResource(R.drawable.bg_servicio_pildora)
                setTextColor(color(R.color.petcare_primary_dark))
                textSize = 13f
                typeface = fuente(R.font.poppins_semibold)
                setPadding(dp(12), dp(13), dp(12), dp(13))
            }
        }
        tarjeta.addView(
            CuadriculaDeOpciones.crear(this, chips, columnas = 2, separacion = dp(10)).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { topMargin = dp(14) }
            },
        )
        return tarjeta
    }

    // ---------------------------------------------------------------------
    // Pestaña Contacto
    // ---------------------------------------------------------------------

    private fun pintarContacto(p: PerfilVeterinariaRespuesta) {
        val cont = binding.contenidoContacto
        cont.removeAllViews()

        cont.addView(titulo(R.string.perfil_horario_titulo))
        val dias = resources.getStringArray(R.array.dias_abreviados)
        val diasDeLaSemana = listOf("lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo")
        val tarjetaHorario = tarjetaSuave()
        val lineasHorario = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        lineasHorario.addView(textoSimple(getString(R.string.perfil_dias_y_horas), tamano = 13f))
        for (grupo in p.horarioSemana) {
            val desde = dias[diasDeLaSemana.indexOf(grupo.desde).coerceAtLeast(0)]
            val hasta = dias[diasDeLaSemana.indexOf(grupo.hasta).coerceAtLeast(0)]
            val rotulo = if (grupo.desde == grupo.hasta) desde else "$desde–$hasta"
            val fila = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(3), 0, 0)
            }
            fila.addView(textoSimple("$rotulo: "))
            fila.addView(textoSimple(FormatosMapa.rangoDeFranjas(this, grupo.franjas), negrita = true))
            if (grupo.incluyeHoy) {
                fila.addView(
                    TextView(this).apply {
                        setText(R.string.perfil_hoy)
                        setTextColor(color(R.color.white))
                        textSize = 11f
                        typeface = fuente(R.font.poppins_semibold)
                        setBackgroundResource(R.drawable.bg_etiqueta_pildora)
                        backgroundTintList = ColorStateList.valueOf(color(R.color.petcare_primary))
                        setPadding(dp(8), dp(1), dp(8), dp(1))
                        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                            .apply { marginStart = dp(8) }
                    },
                )
            }
            lineasHorario.addView(fila)
        }
        tarjetaHorario.addView(iconoEnCuadro(R.drawable.ic_reloj))
        tarjetaHorario.addView(lineasHorario.apply { layoutParams = pesoUno().apply { marginStart = dp(12) } })
        cont.addView(tarjetaHorario)

        // Sin teléfonos ni redes no se muestra este apartado (RF41).
        if (p.telefonos.isEmpty() && p.redes.isEmpty()) return
        cont.addView(titulo(R.string.perfil_contacto_titulo))
        val tarjeta = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_tarjeta_suave)
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }
        if (p.telefonos.isNotEmpty()) {
            // Tocar un teléfono abre el marcador con el número escrito.
            tarjeta.addView(
                filaDeContacto(
                    R.drawable.ic_telefono,
                    getString(R.string.perfil_telefonos),
                    p.telefonos.map { telefono -> LineaContacto("", telefono) { Enlaces.llamar(this, telefono) } },
                ),
            )
        }
        if (p.telefonos.isNotEmpty() && p.redes.isNotEmpty()) {
            tarjeta.addView(
                View(this).apply {
                    setBackgroundColor(color(R.color.petcare_border))
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)).apply {
                        topMargin = dp(12)
                        bottomMargin = dp(12)
                    }
                },
            )
        }
        if (p.redes.isNotEmpty()) {
            // Tocar una red social o web abre el enlace en la aplicación o el navegador.
            tarjeta.addView(
                filaDeContacto(
                    R.drawable.ic_web,
                    getString(R.string.perfil_redes),
                    p.redes.map { enlace ->
                        val (nombre, usuario) = redLegible(enlace)
                        LineaContacto(nombre, usuario) { Enlaces.abrirEnlace(this, enlace) }
                    },
                ),
            )
        }
        cont.addView(tarjeta)
    }

    /** Una línea de contacto: parte normal, parte en negrita y qué hacer al tocarla. */
    private class LineaContacto(val normal: String, val fuerte: String, val alTocar: () -> Unit)

    /** Ícono, un rótulo pequeño y debajo las líneas tocables: cada una con una parte normal y otra en negrita. */
    private fun filaDeContacto(@DrawableRes icono: Int, rotulo: String, lineas: List<LineaContacto>): View {
        val fila = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val columna = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = pesoUno().apply { marginStart = dp(12) }
        }
        columna.addView(textoSimple(rotulo, tamano = 13f))
        for (contacto in lineas) {
            val linea = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(4), 0, dp(4)) // más alto para que sea fácil de tocar
                setOnClickListener { contacto.alTocar() }
            }
            if (contacto.normal.isNotEmpty()) linea.addView(textoSimple("${contacto.normal} "))
            // Se ve en color de acción para indicar que se puede tocar.
            linea.addView(textoSimple(contacto.fuerte, negrita = true, colorId = R.color.petcare_primary_dark))
            columna.addView(linea)
        }
        fila.addView(iconoEnCuadro(icono))
        fila.addView(columna)
        return fila
    }

    /** "https://www.instagram.com/vetpatitas" -> ("Instagram:", "@vetpatitas"); otros enlaces -> ("Web:", "www.sitio.pe"). */
    private fun redLegible(enlace: String): Pair<String, String> {
        val uri = runCatching { URI(enlace) }.getOrNull()
        val host = uri?.host.orEmpty()
        val ruta = uri?.path.orEmpty().trim('/')
        return when {
            host.contains("instagram") -> "Instagram:" to "@$ruta"
            host.contains("facebook") -> "Facebook:" to ruta
            host.isNotEmpty() -> "Web:" to host + if (ruta.isNotEmpty()) "/$ruta" else ""
            else -> "Web:" to enlace
        }
    }

    // ---------------------------------------------------------------------
    // Pestaña Reseñas
    // ---------------------------------------------------------------------

    private fun pintarResenas(p: PerfilVeterinariaRespuesta) {
        val cont = binding.contenidoResenas
        cont.removeAllViews()
        val resumen = p.resenas

        val tarjeta = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_tarjeta_suave)
            setPadding(dp(18), dp(16), dp(18), dp(16))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(14) }
        }
        if (resumen.total == 0) {
            tarjeta.addView(textoSimple(getString(R.string.perfil_sin_resenas), tamano = 15f))
        } else {
            tarjeta.addView(
                TextView(this).apply {
                    text = FormatosMapa.calificacion(resumen.promedio ?: 0.0)
                    setTextColor(color(R.color.petcare_text))
                    textSize = 34f
                    typeface = fuente(R.font.poppins_bold)
                },
            )
            val columna = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { marginStart = dp(14) }
            }
            val estrellas = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            FormatosMapa.pintarEstrellas(estrellas, FormatosMapa.estrellasDe(resumen.promedio), 16)
            columna.addView(estrellas)
            columna.addView(
                textoSimple(
                    if (resumen.total == 1) getString(R.string.perfil_una_resena) else getString(R.string.perfil_varias_resenas, resumen.total),
                    tamano = 13f,
                ),
            )
            tarjeta.addView(columna)
        }
        cont.addView(tarjeta)

        for (resena in resumen.recientes) cont.addView(ResenaFila.crear(cont, resena))

        if (resumen.total > 0) {
            cont.addView(
                TextView(this).apply {
                    setText(R.string.perfil_ver_todas)
                    setTextColor(color(R.color.petcare_primary_dark))
                    textSize = 14f
                    typeface = fuente(R.font.poppins_semibold)
                    gravity = Gravity.CENTER
                    setPadding(0, dp(14), 0, dp(14))
                    setOnClickListener {
                        startActivity(ResenasActivity.intent(this@PerfilVeterinariaActivity, p.id, p.nombre))
                    }
                },
            )
        }
        cont.addView(
            Button(this, null, 0, R.style.BotonPrincipal).apply {
                setText(R.string.perfil_escribir_resena)
                gravity = Gravity.CENTER // un botón creado por código no trae el centrado de los botones del diseño
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)).apply { topMargin = dp(8) }
                setOnClickListener {
                    startActivity(EscribirResenaActivity.intentNueva(this@PerfilVeterinariaActivity, p.id, p.nombre))
                }
            },
        )
    }

    // ---------------------------------------------------------------------
    // Pestaña Campañas
    // ---------------------------------------------------------------------

    private fun pintarCampanas(p: PerfilVeterinariaRespuesta) {
        val cont = binding.contenidoCampanas
        cont.removeAllViews()

        // Filtro por estado (RF42): sin ninguno elegido se ven todas; con uno, solo ese estado; con los dos, todas.
        val filtros = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(14) }
        }
        filtros.addView(chipDeEstado(R.string.mapa_estado_activa, filtroActivas, primero = true) {
            filtroActivas = !filtroActivas
            pintarCampanas(p)
        })
        filtros.addView(chipDeEstado(R.string.mapa_estado_proxima, filtroProximas, primero = false) {
            filtroProximas = !filtroProximas
            pintarCampanas(p)
        })
        cont.addView(filtros)

        val ninguno = !filtroActivas && !filtroProximas
        val visibles = p.campanas
            .filter { ninguno || (it.estado == "activa" && filtroActivas) || (it.estado == "proxima" && filtroProximas) }
            // Las activas primero y luego las próximas, cada grupo por fecha de inicio.
            .sortedWith(compareBy({ it.estado != "activa" }, { it.fechaInicio }))
        if (visibles.isEmpty()) {
            cont.addView(
                textoSimple(getString(R.string.perfil_sin_campanas_estado)).apply {
                    gravity = Gravity.CENTER
                    setPadding(0, dp(28), 0, dp(12))
                },
            )
        }

        for (campana in visibles) {
            val tarjeta = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_tarjeta_borde)
                setPadding(dp(14), dp(14), dp(14), dp(14))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { topMargin = dp(14) }
            }
            val encabezado = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            encabezado.addView(
                ImageView(this).apply {
                    setImageResource(R.drawable.ic_mapa_campana)
                    setBackgroundResource(R.drawable.bg_icono_resultado)
                    backgroundTintList = ColorStateList.valueOf(color(MarcadoresMapa.colorDeCampana(campana.estado)))
                    setPadding(dp(10), dp(10), dp(10), dp(10))
                    layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
                },
            )
            val columna = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = pesoUno().apply { marginStart = dp(12) }
            }
            columna.addView(textoSimple(campana.nombre, tamano = 15f, negrita = true, colorId = R.color.petcare_text))

            val etiquetas = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(5), 0, dp(5))
            }
            val activa = campana.estado == "activa"
            etiquetas.addView(
                etiqueta(
                    if (activa) R.string.mapa_estado_activa else R.string.mapa_estado_proxima,
                    if (activa) R.color.etiqueta_activa_fondo else R.color.etiqueta_proxima_fondo,
                    if (activa) R.color.etiqueta_activa_texto else R.color.etiqueta_proxima_texto,
                ),
            )
            etiquetas.addView(
                etiqueta(
                    if (campana.costo == "gratuita") R.string.mapa_costo_gratuita else R.string.mapa_costo_de_pago,
                    R.color.petcare_field,
                    R.color.petcare_text_muted,
                    separacion = dp(6),
                ),
            )
            columna.addView(etiquetas)
            val fechas = if (campana.fechaInicio == campana.fechaFin) {
                FormatosMapa.fechaCorta(campana.fechaInicio)
            } else {
                "${FormatosMapa.fechaCorta(campana.fechaInicio)} – ${FormatosMapa.fechaCorta(campana.fechaFin)}"
            }
            columna.addView(textoSimple(fechas, tamano = 13f))
            encabezado.addView(columna)

            // Al tocar la tarjeta se despliega el detalle de la campaña (RF43) y se vuelve a plegar al tocarla otra vez.
            val flecha = ImageView(this).apply {
                setImageResource(R.drawable.ic_desplegar)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                layoutParams = LinearLayout.LayoutParams(dp(24), dp(24)).apply { marginStart = dp(8) }
            }
            encabezado.addView(flecha)
            tarjeta.addView(encabezado)

            val detalle = CuerpoCampana.crear(this, campana.comoCuerpo()).apply { isVisible = false }
            tarjeta.addView(detalle)

            fun mostrarDetalle(abierto: Boolean) {
                detalle.isVisible = abierto
                flecha.rotation = if (abierto) 180f else 0f
            }
            mostrarDetalle(campana.id in campanasAbiertas)
            tarjeta.setOnClickListener {
                if (!campanasAbiertas.remove(campana.id)) campanasAbiertas.add(campana.id)
                mostrarDetalle(campana.id in campanasAbiertas)
            }
            cont.addView(tarjeta)
            if (campana.id == campanaResaltada) tarjetaResaltada = tarjeta
        }
    }

    private fun chipDeEstado(@StringRes texto: Int, elegido: Boolean, primero: Boolean, alTocar: () -> Unit) = TextView(this).apply {
        setText(texto)
        gravity = Gravity.CENTER
        // Igual que los demás filtros de la app: elegido con borde verde azulado grueso, sin elegir con borde gris fino.
        setBackgroundResource(if (elegido) R.drawable.bg_chip_opcion_activo else R.drawable.bg_chip_opcion)
        setTextColor(color(if (elegido) R.color.petcare_text else R.color.petcare_text_muted))
        textSize = 14f
        typeface = fuente(R.font.poppins_semibold)
        setPadding(dp(20), 0, dp(20), 0)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(40)).apply { marginStart = if (primero) 0 else dp(10) }
        setOnClickListener { alTocar() }
    }

    /** Distancia desde la parte de arriba de [ancestro] hasta [vista], para llevar el desplazamiento hasta ella. */
    private fun distanciaDesde(ancestro: View, vista: View): Int {
        var total = 0
        var actual: View? = vista
        while (actual != null && actual !== ancestro) {
            total += actual.top
            actual = actual.parent as? View
        }
        return total
    }

    private fun etiqueta(@StringRes texto: Int, @ColorRes fondo: Int, @ColorRes letra: Int, separacion: Int = 0) =
        TextView(this).apply {
            setText(texto)
            setBackgroundResource(R.drawable.bg_etiqueta_pildora)
            backgroundTintList = ColorStateList.valueOf(color(fondo))
            setTextColor(color(letra))
            textSize = 12f
            typeface = fuente(R.font.poppins_semibold)
            setPadding(dp(10), dp(2), dp(10), dp(2))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { marginStart = separacion }
        }

    // ---------------------------------------------------------------------
    // Piezas comunes
    // ---------------------------------------------------------------------

    private fun titulo(@StringRes texto: Int) = TextView(this).apply {
        setText(texto)
        setTextColor(color(R.color.petcare_text_muted))
        textSize = 12f
        letterSpacing = 0.04f
        typeface = fuente(R.font.poppins_semibold)
        setPadding(0, dp(20), 0, dp(8))
    }

    private fun textoSimple(
        texto: String,
        tamano: Float = 14f,
        negrita: Boolean = false,
        @ColorRes colorId: Int = if (negrita) R.color.petcare_text else R.color.petcare_text_muted,
    ) = TextView(this).apply {
        text = texto
        setTextColor(color(colorId))
        textSize = tamano
        // Normal y negrita usan la misma familia (Poppins): con fuentes distintas las líneas se desalinean.
        typeface = fuente(if (negrita) R.font.poppins_semibold else R.font.poppins_regular)
    }

    private fun tarjetaSuave() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setBackgroundResource(R.drawable.bg_tarjeta_suave)
        setPadding(dp(14), dp(14), dp(14), dp(14))
    }

    private fun iconoEnCuadro(@DrawableRes icono: Int, @DrawableRes fondo: Int = R.drawable.bg_icono_info) = ImageView(this).apply {
        setImageResource(icono)
        setBackgroundResource(fondo)
        imageTintList = ColorStateList.valueOf(color(R.color.petcare_primary_dark))
        setPadding(dp(10), dp(10), dp(10), dp(10))
        layoutParams = LinearLayout.LayoutParams(dp(42), dp(42))
    }

    /** Encabezado de una categoría de servicios: su ícono en un cuadro rectangular, el nombre y cuántos servicios tiene. */
    private fun filaConIcono(@DrawableRes icono: Int, titulo: String, subtitulo: String): View {
        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        fila.addView(iconoEnCuadro(icono, R.drawable.bg_icono_categoria))
        val columna = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = pesoUno().apply { marginStart = dp(12) }
        }
        columna.addView(textoSimple(titulo, tamano = 15f, negrita = true))
        columna.addView(textoSimple(subtitulo, tamano = 12f))
        fila.addView(columna)
        return fila
    }

    private fun pesoUno() = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

    private fun fuente(id: Int): Typeface? = ResourcesCompat.getFont(this, id)

    private fun color(@ColorRes id: Int) = ContextCompat.getColor(this, id)

    private fun dp(valor: Int) = (valor * resources.displayMetrics.density).toInt()

    companion object {
        private const val EXTRA_ID = "id"
        private const val EXTRA_LAT = "lat"
        private const val EXTRA_LNG = "lng"
        private const val EXTRA_CAMPANA = "campana"

        /**
         * [lat] y [lng] son la ubicación del usuario, si se conoce: con ellas el perfil muestra la distancia.
         * Con [campanaId] se abre en la pestaña Campañas, con esa campaña desplegada (RF43).
         */
        fun intent(contexto: Context, id: Int, lat: Double?, lng: Double?, campanaId: Int? = null): Intent =
            Intent(contexto, PerfilVeterinariaActivity::class.java).apply {
                putExtra(EXTRA_ID, id)
                if (campanaId != null) putExtra(EXTRA_CAMPANA, campanaId)
                if (lat != null && lng != null) {
                    putExtra(EXTRA_LAT, lat)
                    putExtra(EXTRA_LNG, lng)
                }
            }
    }
}
