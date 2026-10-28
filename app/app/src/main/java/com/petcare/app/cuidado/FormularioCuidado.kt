package com.petcare.app.cuidado

import android.app.DatePickerDialog
import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.petcare.app.R
import com.petcare.app.databinding.VistaFormularioCuidadoBinding
import com.petcare.app.mascota.comoFechaLegible
import java.time.LocalDate
import java.time.ZoneId

/**
 * Lógica del formulario de configuración de un cuidado: modo de término, frecuencia, fechas,
 * aviso anticipado y nota. Se encarga de mostrar u ocultar campos según el modo y de validar
 * con los mensajes del ERS (RF11 y RF12). Es independiente de la pantalla que lo contiene.
 */
class FormularioCuidado(
    private val contexto: Context,
    private val vista: VistaFormularioCuidadoBinding,
) {
    /** Unidad de la frecuencia: cómo se llama en el servidor y cómo se suma a una fecha. */
    private class Unidad(val clave: String, val sumar: (LocalDate, Int) -> LocalDate)

    private val unidades = listOf(
        Unidad("dias") { f, v -> f.plusDays(v.toLong()) },
        Unidad("semanas") { f, v -> f.plusWeeks(v.toLong()) },
        Unidad("meses") { f, v -> f.plusMonths(v.toLong()) },
        Unidad("anios") { f, v -> f.plusYears(v.toLong()) },
    )

    private var modo: String? = null
    private var unidad = unidades.first { it.clave == "meses" }
    private var fechaInicio: LocalDate? = null
    private var fechaLimite: LocalDate? = null

    /** Al editar, la fecha de inicio puede ser pasada (por ejemplo, un cuidado que ya venció). */
    private var permitirFechaPasada = false

    /** En un cuidado personalizado el usuario escribe el nombre y la descripción. */
    private val esPersonalizado get() = vista.bloqueNombre.visibility == View.VISIBLE

    val nombrePersonalizado: String get() = vista.campoNombre.text.toString().trim()
    val descripcionPersonalizada: String get() = vista.campoDescripcion.text.toString().trim()

    private val botonesDeModo = mapOf(
        "una_vez" to vista.modoUnaVez,
        "varias_veces" to vista.modoVariasVeces,
        "hasta_fecha" to vista.modoHastaFecha,
        "indefinido" to vista.modoIndefinido,
    )

    private val botonesDeUnidad = mapOf(
        "dias" to vista.unidadDias,
        "semanas" to vista.unidadSemanas,
        "meses" to vista.unidadMeses,
        "anios" to vista.unidadAnios,
    )

    init {
        botonesDeModo.forEach { (clave, boton) -> boton.setOnClickListener { elegirModo(clave) } }
        botonesDeUnidad.forEach { (clave, boton) -> boton.setOnClickListener { elegirUnidad(clave) } }
        vista.verMasInformacion.setOnClickListener { mostrarInformacion(vista.bloqueInformacion.visibility != View.VISIBLE) }
        vista.campoInicio.setOnClickListener { abrirCalendarioInicio() }
        vista.campoLimite.setOnClickListener { abrirCalendarioLimite() }
        vista.campoFrecuencia.doAfterTextChanged { actualizarAproximado() }
    }

    /** Muestra un cuidado recomendado (nombre y descripción no se editan) con el formulario vacío. */
    fun cargarRecomendacion(recomendacion: RecomendacionRespuesta) {
        mostrarTarjeta(recomendacion.nombre, recomendacion.descripcion, recomendacion.importancia)
        permitirFechaPasada = false
        limpiar()
    }

    /** Formulario vacío para agregar un cuidado personalizado (RF12). */
    fun cargarPersonalizado() {
        mostrarCamposDeNombre()
        permitirFechaPasada = false
        limpiar()
        vista.campoNombre.setText("")
        vista.campoDescripcion.setText("")
    }

    /**
     * Formulario con los datos de un cuidado existente (RF14). En uno recomendado el nombre y la
     * descripción están bloqueados; en uno personalizado también se pueden editar.
     */
    fun cargarEdicion(cuidado: CuidadoRespuesta) {
        if (cuidado.esRecomendado) {
            mostrarTarjeta(cuidado.nombre, cuidado.descripcion, cuidado.importancia.orEmpty())
        } else {
            mostrarCamposDeNombre()
        }
        permitirFechaPasada = true
        limpiar()

        if (!cuidado.esRecomendado) {
            vista.campoNombre.setText(cuidado.nombre)
            vista.campoDescripcion.setText(cuidado.descripcion)
        }
        elegirModo(cuidado.modoTermino)
        cuidado.frecuenciaUnidad?.let { elegirUnidad(it) }
        cuidado.frecuenciaValor?.let { vista.campoFrecuencia.setText(it.toString()) }
        cuidado.totalRepeticiones?.let { vista.campoVeces.setText(it.toString()) }
        LocalDate.parse(cuidado.fechaRecurrente).let {
            fechaInicio = it
            fijarFecha(vista.campoInicio, it)
        }
        cuidado.fechaLimite?.let {
            val limite = LocalDate.parse(it)
            fechaLimite = limite
            fijarFecha(vista.campoLimite, limite)
        }
        vista.interruptorAviso.isChecked = cuidado.avisoAnticipado
        vista.campoNota.setText(cuidado.nota.orEmpty())
        actualizarAproximado()
    }

    private fun mostrarTarjeta(nombre: String, descripcion: String, importancia: String) {
        vista.bloqueNombre.visibility = View.GONE
        vista.tarjetaRecomendado.visibility = View.VISIBLE
        vista.nombreCuidado.text = nombre
        vista.descripcionCuidado.text = descripcion
        vista.importanciaCuidado.text = importancia
    }

    private fun mostrarCamposDeNombre() {
        vista.tarjetaRecomendado.visibility = View.GONE
        vista.bloqueNombre.visibility = View.VISIBLE
    }

    private fun limpiar() {
        modo = null
        fechaInicio = null
        fechaLimite = null

        botonesDeModo.values.forEach { it.isSelected = false }
        vista.bloqueFrecuencia.visibility = View.GONE
        vista.bloqueVeces.visibility = View.GONE
        vista.bloqueLimite.visibility = View.GONE
        vista.textoAproximado.visibility = View.GONE
        mostrarInformacion(false)

        vista.campoFrecuencia.setText("")
        vista.campoVeces.setText("")
        vista.campoNota.setText("")
        elegirUnidad("meses")
        quitarFecha(vista.campoInicio)
        quitarFecha(vista.campoLimite)
        vista.interruptorAviso.isChecked = true

        ocultarErrores()
    }

    /**
     * Valida el formulario. Si todo está bien devuelve la configuración; si no, deja marcados
     * los campos con error (con el mensaje del ERS) y devuelve null.
     */
    fun leer(): ConfiguracionCuidado? {
        ocultarErrores()
        var correcto = true
        val modoElegido = modo

        if (esPersonalizado && nombrePersonalizado.isEmpty()) {
            mostrarError(vista.errorNombre, R.string.error_campo_obligatorio)
            correcto = false
        }

        if (modoElegido == null) {
            mostrarError(vista.errorModo, R.string.error_campo_obligatorio)
            correcto = false
        }

        var frecuencia: Int? = null
        if (modoElegido != null && modoElegido != "una_vez") {
            frecuencia = leerEntero(vista.campoFrecuencia, vista.errorFrecuencia)
            if (frecuencia == null) correcto = false
        }

        var veces: Int? = null
        if (modoElegido == "varias_veces") {
            veces = leerEntero(vista.campoVeces, vista.errorVeces)
            if (veces == null) correcto = false
        }

        val inicio = fechaInicio
        if (inicio == null) {
            mostrarError(vista.errorInicio, R.string.error_campo_obligatorio)
            correcto = false
        } else if (!permitirFechaPasada && inicio.isBefore(LocalDate.now())) {
            mostrarError(vista.errorInicio, R.string.error_inicio_pasada)
            correcto = false
        }

        val limite = fechaLimite
        if (modoElegido == "hasta_fecha") {
            if (limite == null) {
                mostrarError(vista.errorLimite, R.string.error_campo_obligatorio)
                correcto = false
            } else if (inicio != null && !limite.isAfter(inicio)) {
                mostrarError(vista.errorLimite, R.string.error_limite_posterior)
                correcto = false
            }
        }

        if (!correcto || modoElegido == null || inicio == null) return null

        return ConfiguracionCuidado(
            modoTermino = modoElegido,
            frecuenciaValor = frecuencia,
            frecuenciaUnidad = if (modoElegido == "una_vez") null else unidad.clave,
            totalRepeticiones = veces,
            fechaLimite = if (modoElegido == "hasta_fecha") limite?.toString() else null,
            fechaInicio = inicio.toString(),
            avisoAnticipado = vista.interruptorAviso.isChecked,
            nota = vista.campoNota.text.toString().trim().ifEmpty { null },
        )
    }

    /** "Ver más información" / "Ocultar información": despliega o recoge la descripción e importancia. */
    private fun mostrarInformacion(abierta: Boolean) {
        vista.bloqueInformacion.visibility = if (abierta) View.VISIBLE else View.GONE
        vista.flechaInformacion.rotation = if (abierta) 180f else 0f
        vista.textoVerMas.setText(if (abierta) R.string.cuidado_ocultar else R.string.cuidado_ver_mas)
    }

    private fun elegirModo(nuevo: String) {
        modo = nuevo
        botonesDeModo.forEach { (clave, boton) -> boton.isSelected = clave == nuevo }
        vista.errorModo.visibility = View.GONE
        // Con "una vez" no hay frecuencia; el número de veces y la fecha límite dependen del modo.
        vista.bloqueFrecuencia.visibility = if (nuevo == "una_vez") View.GONE else View.VISIBLE
        vista.bloqueVeces.visibility = if (nuevo == "varias_veces") View.VISIBLE else View.GONE
        vista.bloqueLimite.visibility = if (nuevo == "hasta_fecha") View.VISIBLE else View.GONE
        actualizarAproximado()
    }

    private fun elegirUnidad(clave: String) {
        unidad = unidades.first { it.clave == clave }
        botonesDeUnidad.forEach { (k, boton) -> boton.isSelected = k == clave }
        actualizarAproximado()
    }

    private fun abrirCalendarioInicio() {
        val minimo = if (permitirFechaPasada) LocalDate.now().minusYears(10) else LocalDate.now()
        abrirCalendario(fechaInicio, minimo) { fecha ->
            fechaInicio = fecha
            fijarFecha(vista.campoInicio, fecha)
            vista.errorInicio.visibility = View.GONE
            actualizarAproximado()
        }
    }

    private fun abrirCalendarioLimite() {
        val minimo = (fechaInicio ?: LocalDate.now()).plusDays(1)
        abrirCalendario(fechaLimite, minimo) { fecha ->
            fechaLimite = fecha
            fijarFecha(vista.campoLimite, fecha)
            vista.errorLimite.visibility = View.GONE
            actualizarAproximado()
        }
    }

    private fun abrirCalendario(actual: LocalDate?, minimo: LocalDate, alElegir: (LocalDate) -> Unit) {
        val base = actual?.takeIf { !it.isBefore(minimo) } ?: minimo
        val dialogo = DatePickerDialog(
            contexto,
            { _, anio, mes, dia -> alElegir(LocalDate.of(anio, mes + 1, dia)) },
            base.year,
            base.monthValue - 1,
            base.dayOfMonth,
        )
        dialogo.datePicker.minDate = minimo.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        dialogo.show()
    }

    private fun fijarFecha(campo: TextView, fecha: LocalDate) {
        campo.text = fecha.toString().comoFechaLegible()
        campo.setTextColor(ContextCompat.getColor(contexto, R.color.petcare_text))
    }

    private fun quitarFecha(campo: TextView) {
        campo.setText(R.string.hint_fecha)
        campo.setTextColor(ContextCompat.getColor(contexto, R.color.hint_gris))
    }

    /** "Hasta una fecha": calcula cuántas veces caben entre el inicio y la fecha límite (RF12). */
    private fun actualizarAproximado() {
        val inicio = fechaInicio
        val limite = fechaLimite
        val frecuencia = vista.campoFrecuencia.text.toString().toIntOrNull()
        if (modo != "hasta_fecha" || inicio == null || limite == null || frecuencia == null ||
            frecuencia !in 1..MAXIMO || !limite.isAfter(inicio)
        ) {
            vista.textoAproximado.visibility = View.GONE
            return
        }
        var repeticiones = 0
        var fecha: LocalDate = inicio
        while (!fecha.isAfter(limite) && repeticiones < TOPE_CALCULO) {
            repeticiones++
            fecha = unidad.sumar(fecha, frecuencia)
        }
        vista.textoAproximado.text =
            contexto.resources.getQuantityString(R.plurals.cuidado_aproximado, repeticiones, repeticiones)
        vista.textoAproximado.visibility = View.VISIBLE
    }

    private fun leerEntero(campo: EditText, error: TextView): Int? {
        val texto = campo.text.toString().trim()
        if (texto.isEmpty()) {
            mostrarError(error, R.string.error_campo_obligatorio)
            return null
        }
        val numero = texto.toIntOrNull()
        if (numero == null || numero !in 1..MAXIMO) {
            mostrarError(error, R.string.error_entero)
            return null
        }
        return numero
    }

    private fun mostrarError(campo: TextView, @StringRes mensaje: Int) {
        campo.setText(mensaje)
        campo.visibility = View.VISIBLE
    }

    private fun ocultarErrores() {
        listOf(vista.errorNombre, vista.errorModo, vista.errorFrecuencia, vista.errorVeces, vista.errorLimite, vista.errorInicio)
            .forEach { it.visibility = View.GONE }
    }

    private companion object {
        const val MAXIMO = 999
        const val TOPE_CALCULO = 10_000
    }
}
