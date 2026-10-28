package com.petcare.app.cuidado

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.StringRes
import com.petcare.app.R
import com.petcare.app.databinding.DialogoBaseBinding
import com.petcare.app.databinding.DialogoCompletarBinding
import com.petcare.app.databinding.DialogoFechaBaseBinding
import com.petcare.app.databinding.DialogoNotaBinding
import com.petcare.app.mascota.comoFechaLegible
import java.time.LocalDate

/** Diálogos del cuidado: completar (RF16), fecha base (RF17) y nota de una actividad (RF18). */

/**
 * Diálogo con el diseño de PetCare: título, contenido y los botones Cancelar y Aceptar.
 * [alAceptar] recibe una función para cerrarlo, de modo que un diálogo con errores pueda quedarse abierto.
 */
private fun Context.mostrarDialogo(
    titulo: String,
    contenido: View,
    @StringRes textoAceptar: Int = R.string.aceptar,
    alAceptar: (cerrar: () -> Unit) -> Unit,
) {
    val marco = DialogoBaseBinding.inflate(LayoutInflater.from(this))
    val dialogo = Dialog(this)

    marco.titulo.text = titulo
    marco.contenido.addView(contenido)
    marco.botonAceptar.setText(textoAceptar)
    marco.botonCancelar.setOnClickListener { dialogo.dismiss() }
    marco.botonAceptar.setOnClickListener { alAceptar { dialogo.dismiss() } }

    dialogo.setContentView(marco.root)
    dialogo.window?.apply {
        // El fondo transparente con margen deja ver las esquinas redondeadas y un borde a cada lado.
        val margen = (24 * resources.displayMetrics.density).toInt()
        setBackgroundDrawable(InsetDrawable(ColorDrawable(Color.TRANSPARENT), margen))
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }
    dialogo.show()
}

/** "¿Cuándo se realizó …?": fecha de realización (no futura) y nota opcional. */
fun Context.dialogoCompletar(cuidado: CuidadoRespuesta, alAceptar: (fecha: LocalDate, nota: String?) -> Unit) {
    val vista = DialogoCompletarBinding.inflate(LayoutInflater.from(this))
    var fecha = LocalDate.now()
    vista.campoFecha.text = fecha.toString().comoFechaLegible()

    vista.campoFecha.setOnClickListener {
        val dialogoFecha = DatePickerDialog(
            this,
            { _, anio, mes, dia ->
                fecha = LocalDate.of(anio, mes + 1, dia)
                vista.campoFecha.text = fecha.toString().comoFechaLegible()
            },
            fecha.year,
            fecha.monthValue - 1,
            fecha.dayOfMonth,
        )
        // No se puede registrar un cuidado como realizado en el futuro.
        dialogoFecha.datePicker.maxDate = System.currentTimeMillis()
        dialogoFecha.show()
    }

    mostrarDialogo(getString(R.string.completar_titulo, cuidado.nombre), vista.root) { cerrar ->
        cerrar()
        alAceptar(fecha, vista.campoNota.text.toString().trim().ifEmpty { null })
    }
}

/**
 * RF17: pregunta qué fecha usar como base para calcular la próxima, mostrando el resultado de cada una.
 * Devuelve "planificada" o "realizacion". Si se cierra sin elegir, no se registra nada.
 */
fun Context.dialogoFechaBase(
    cuidado: CuidadoRespuesta,
    planificada: LocalDate,
    realizacion: LocalDate,
    alAceptar: (base: String) -> Unit,
) {
    val vista = DialogoFechaBaseBinding.inflate(LayoutInflater.from(this))

    fun detalle(base: LocalDate): String {
        val proxima = cuidado.proximaFechaDesde(base)?.toString()?.comoFechaLegible().orEmpty()
        return getString(R.string.fecha_base_detalle, base.toString().comoFechaCorta(), proxima)
    }
    vista.detallePlanificada.text = detalle(planificada)
    vista.detalleRealizacion.text = detalle(realizacion)

    var base = "planificada"
    fun elegir(nueva: String) {
        base = nueva
        val planificadaElegida = nueva == "planificada"
        vista.opcionPlanificada.isSelected = planificadaElegida
        vista.radioPlanificada.isSelected = planificadaElegida
        vista.opcionRealizacion.isSelected = !planificadaElegida
        vista.radioRealizacion.isSelected = !planificadaElegida
    }
    vista.opcionPlanificada.setOnClickListener { elegir("planificada") }
    vista.opcionRealizacion.setOnClickListener { elegir("realizacion") }
    elegir(base)

    mostrarDialogo(getString(R.string.fecha_base_titulo), vista.root) { cerrar ->
        cerrar()
        alAceptar(base)
    }
}

/** Agregar o editar la nota de una actividad. No acepta una nota vacía (para quitarla está "Eliminar nota"). */
fun Context.dialogoNota(notaActual: String?, alGuardar: (nota: String) -> Unit) {
    val vista = DialogoNotaBinding.inflate(LayoutInflater.from(this))
    vista.campoNota.setText(notaActual.orEmpty())
    vista.campoNota.setSelection(vista.campoNota.text.length)

    mostrarDialogo(getString(R.string.actividad_nota_titulo), vista.root, R.string.boton_guardar) { cerrar ->
        val nota = vista.campoNota.text.toString().trim()
        if (nota.isEmpty()) {
            vista.campoNota.error = getString(R.string.error_campo_obligatorio)
        } else {
            cerrar()
            alGuardar(nota)
        }
    }
}
