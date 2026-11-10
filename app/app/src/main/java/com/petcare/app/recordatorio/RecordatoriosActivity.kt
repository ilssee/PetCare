package com.petcare.app.recordatorio

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import com.petcare.app.R
import com.petcare.app.common.Resultado
import com.petcare.app.common.paraMostrar
import com.petcare.app.common.respetarBarrasYTeclado
import com.petcare.app.databinding.ActivityRecordatoriosBinding
import kotlinx.coroutines.launch

/**
 * Configuración de recordatorios (RF20): los días de la semana en que el usuario quiere recibir el aviso
 * anticipado. Si no elige ninguno, se usan todos los días. La hora es siempre las 8:00 a. m.
 */
class RecordatoriosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecordatoriosBinding
    private val repositorio = RecordatorioRepository()

    private val botones = mutableListOf<TextView>()
    private val elegidos = BooleanArray(7) // de lunes (0) a domingo (6)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRecordatoriosBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.raiz.respetarBarrasYTeclado()

        binding.botonVolver.setOnClickListener { finish() }
        binding.botonGuardar.setOnClickListener { guardar() }
        crearBotonesDeDias()
        actualizarAviso()
        cargar()
    }

    private fun crearBotonesDeDias() {
        val etiquetas = resources.getStringArray(R.array.dias_cortos)
        etiquetas.forEachIndexed { posicion, etiqueta ->
            val boton = TextView(this).apply {
                text = etiqueta
                gravity = Gravity.CENTER
                textSize = 13f
                typeface = ResourcesCompat.getFont(context, R.font.poppins_semibold)
                setBackgroundResource(R.drawable.bg_segmento)
                setTextColor(context.getColorStateList(R.color.texto_segmento))
                layoutParams = android.widget.LinearLayout.LayoutParams(0, (44 * resources.displayMetrics.density).toInt(), 1f)
                    .apply { marginStart = if (posicion == 0) 0 else (4 * resources.displayMetrics.density).toInt() }
                setOnClickListener {
                    elegidos[posicion] = !elegidos[posicion]
                    isSelected = elegidos[posicion]
                    actualizarAviso()
                }
            }
            botones.add(boton)
            binding.dias.addView(boton)
        }
    }

    private fun cargar() {
        lifecycleScope.launch {
            when (val resultado = repositorio.obtener()) {
                is Resultado.Exito -> {
                    // Con los siete días guardados (el valor por defecto) no se marca ninguno.
                    val guardados = resultado.dato.dias
                    if (guardados.size < DIAS.size) {
                        DIAS.forEachIndexed { posicion, dia ->
                            elegidos[posicion] = dia in guardados
                            botones[posicion].isSelected = elegidos[posicion]
                        }
                    }
                    actualizarAviso()
                }
                is Resultado.Fallo -> mostrarError(resultado.paraMostrar(this@RecordatoriosActivity))
            }
        }
    }

    /**
     * El aviso explica lo que se va a hacer con la configuración actual: sin días marcados, el día antes
     * de cada vencimiento; con días, solo esos, y un ejemplo de cuándo llegaría el aviso.
     */
    private fun actualizarAviso() {
        val marcados = DIAS.indices.filter { elegidos[it] }
        if (marcados.isEmpty()) {
            binding.textoAviso.text = HtmlCompat.fromHtml(getString(R.string.recordatorios_aviso_todos), HtmlCompat.FROM_HTML_MODE_COMPACT)
            binding.textoEjemplo.visibility = View.GONE
            return
        }

        val plural = resources.getStringArray(R.array.dias_plural)
        val nombres = marcados.map { plural[it] }
        // "lunes", "lunes y miércoles", "lunes, martes y miércoles"
        val lista = if (nombres.size == 1) nombres.first() else nombres.dropLast(1).joinToString(", ") + " y " + nombres.last()
        binding.textoAviso.text = HtmlCompat.fromHtml(
            getString(R.string.recordatorios_aviso_dias, lista),
            HtmlCompat.FROM_HTML_MODE_COMPACT,
        )

        // Con los siete días marcados no hace falta ejemplo: se avisa siempre el día antes.
        if (marcados.size == DIAS.size) {
            binding.textoEjemplo.visibility = View.GONE
        } else {
            val singular = resources.getStringArray(R.array.dias_singular)
            binding.textoEjemplo.text = getString(R.string.recordatorios_ejemplo, singular[diaDeAvisoSiVenceElMiercoles(marcados)])
            binding.textoEjemplo.visibility = View.VISIBLE
        }
    }

    /**
     * Posición (lunes = 0) del día en que llegaría el aviso de un cuidado que vence el miércoles:
     * el martes si está elegido; si no, el día elegido más cercano hacia atrás. Es la misma regla del servidor.
     */
    private fun diaDeAvisoSiVenceElMiercoles(marcados: List<Int>): Int {
        var dia = MARTES
        repeat(DIAS.size) {
            if (dia in marcados) return dia
            dia = (dia + DIAS.size - 1) % DIAS.size
        }
        return dia
    }

    private fun guardar() {
        binding.textoError.visibility = View.GONE
        binding.botonGuardar.isEnabled = false
        // Sin ningún día marcado se envía una lista vacía y el servidor restablece todos (RF20).
        val dias = DIAS.filterIndexed { posicion, _ -> elegidos[posicion] }
        lifecycleScope.launch {
            when (val resultado = repositorio.guardar(dias)) {
                is Resultado.Exito -> {
                    Toast.makeText(this@RecordatoriosActivity, resultado.dato.mensaje, Toast.LENGTH_SHORT).show()
                    finish()
                }
                is Resultado.Fallo -> {
                    mostrarError(resultado.paraMostrar(this@RecordatoriosActivity))
                    binding.botonGuardar.isEnabled = true
                }
            }
        }
    }

    private fun mostrarError(mensaje: String) {
        binding.textoError.text = mensaje
        binding.textoError.visibility = View.VISIBLE
    }

    private companion object {
        /** Los días como los guarda el servidor, de lunes a domingo. */
        val DIAS = listOf("lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo")
        const val MARTES = 1
    }
}
