package com.petcare.app.mascota

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.petcare.app.R
import com.petcare.app.common.Imagenes
import com.petcare.app.common.Resultado
import com.petcare.app.common.paraMostrar
import com.petcare.app.databinding.ActivityMascotaFormBinding
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Formulario para registrar una mascota nueva o editar una existente (si recibe EXTRA_ID).
 * Obligatorios: nombre, especie y sexo. Opcionales: raza, fecha de nacimiento y peso (ERS RF05).
 */
class MascotaFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMascotaFormBinding
    private val repositorio = MascotaRepository()

    private var mascotaId = 0
    private var especie: String? = null
    private var sexo: String? = null
    private var fechaNacimiento: String? = null // AAAA-MM-DD

    // Foto: la elegida ahora (ya preparada), la que ya tenía en el servidor y si se pidió quitarla.
    private var fotoNueva: ByteArray? = null
    private var fotoActual: String? = null
    private var quitarFotoActual = false

    // Selector de fotos de Android: no necesita pedir permisos al usuario.
    private val elegirFoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            lifecycleScope.launch {
                val datos = Imagenes.prepararFoto(this@MascotaFormActivity, uri)
                if (datos == null) {
                    mostrarError(getString(R.string.foto_error_leer))
                } else {
                    fotoNueva = datos
                    quitarFotoActual = false
                    actualizarFoto()
                }
            }
        }
    }

    private val editando get() = mascotaId != 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMascotaFormBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.raiz) { vista, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(0, barras.top, 0, barras.bottom)
            insets
        }

        mascotaId = intent.getIntExtra(EXTRA_ID, 0)

        binding.botonVolver.setOnClickListener { finish() }
        binding.tarjetaPerro.setOnClickListener { elegirEspecie("perro") }
        binding.tarjetaGato.setOnClickListener { elegirEspecie("gato") }
        binding.tarjetaConejo.setOnClickListener { elegirEspecie("conejo") }
        binding.sexoMacho.setOnClickListener { elegirSexo("macho") }
        binding.sexoHembra.setOnClickListener { elegirSexo("hembra") }
        binding.campoFecha.setOnClickListener { abrirCalendario() }
        binding.botonQuitarFecha.setOnClickListener { quitarFecha() }
        binding.botonGuardar.setOnClickListener { guardar() }
        val pedirFoto = View.OnClickListener {
            elegirFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.fotoMascota.setOnClickListener(pedirFoto)
        binding.textoFoto.setOnClickListener(pedirFoto)
        binding.quitarFoto.setOnClickListener {
            fotoNueva = null
            if (fotoActual != null) quitarFotoActual = true
            actualizarFoto()
        }
        actualizarFoto()

        if (editando) {
            binding.titulo.setText(R.string.mascota_editar_titulo)
            binding.botonGuardar.setText(R.string.mascota_guardar_cambios)
            // La especie no se puede cambiar: de ella dependen los cuidados recomendados.
            listOf(binding.tarjetaPerro, binding.tarjetaGato, binding.tarjetaConejo).forEach {
                it.isEnabled = false
                it.alpha = 0.6f
            }
            cargarMascota()
        }
    }

    private fun cargarMascota() {
        lifecycleScope.launch {
            when (val resultado = repositorio.obtener(mascotaId)) {
                is Resultado.Exito -> {
                    val m = resultado.dato
                    elegirEspecie(m.especie)
                    elegirSexo(m.sexo)
                    m.fechaNacimiento?.let { fijarFecha(it) }
                    binding.campoNombre.setText(m.nombre)
                    binding.campoRaza.setText(m.raza ?: "")
                    binding.campoPeso.setText(m.peso?.comoPeso() ?: "")
                    fotoActual = m.foto
                    actualizarFoto()
                }
                is Resultado.Fallo -> mostrarError(resultado.paraMostrar(this@MascotaFormActivity))
            }
        }
    }

    private fun elegirEspecie(nueva: String) {
        especie = nueva
        binding.errorEspecie.visibility = View.GONE
        binding.tarjetaPerro.isSelected = nueva == "perro"
        binding.tarjetaGato.isSelected = nueva == "gato"
        binding.tarjetaConejo.isSelected = nueva == "conejo"
        actualizarFoto()
    }

    /** Muestra la foto elegida o la que ya tenía; si no hay ninguna, la ilustración de la especie. */
    private fun actualizarFoto() {
        val imagen = binding.fotoMascota
        val nueva = fotoNueva
        val actual = fotoActual
        imagen.imageTintList = null
        when {
            nueva != null -> {
                imagen.scaleType = ImageView.ScaleType.CENTER_CROP
                imagen.setPadding(0, 0, 0, 0)
                imagen.setImageBitmap(BitmapFactory.decodeByteArray(nueva, 0, nueva.size))
            }
            actual != null && !quitarFotoActual -> {
                imagen.scaleType = ImageView.ScaleType.CENTER_CROP
                imagen.setPadding(0, 0, 0, 0)
                imagen.load(actual.comoUrlFoto())
            }
            else -> {
                val elegida = especie
                imagen.scaleType = ImageView.ScaleType.FIT_CENTER
                imagen.setPadding(16, 16, 16, 16)
                if (elegida != null) {
                    imagen.setImageResource(Especies.ilustracion(elegida))
                } else {
                    imagen.setImageResource(R.drawable.ic_huella)
                    imagen.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.hint_gris))
                }
            }
        }
        val hayFoto = nueva != null || (actual != null && !quitarFotoActual)
        binding.quitarFoto.visibility = if (hayFoto) View.VISIBLE else View.GONE
    }

    private fun elegirSexo(nuevo: String) {
        sexo = nuevo
        binding.errorSexo.visibility = View.GONE
        binding.sexoMacho.isSelected = nuevo == "macho"
        binding.sexoHembra.isSelected = nuevo == "hembra"
    }

    private fun fijarFecha(fecha: String) {
        fechaNacimiento = fecha
        binding.campoFecha.text = fecha.comoFechaLegible()
        binding.campoFecha.setTextColor(ContextCompat.getColor(this, R.color.petcare_text))
        binding.botonQuitarFecha.visibility = View.VISIBLE
    }

    private fun quitarFecha() {
        fechaNacimiento = null
        binding.campoFecha.setText(R.string.hint_fecha)
        binding.campoFecha.setTextColor(ContextCompat.getColor(this, R.color.hint_gris))
        binding.botonQuitarFecha.visibility = View.GONE
    }

    private fun abrirCalendario() {
        val inicial = fechaNacimiento?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()
        val dialogo = DatePickerDialog(
            this,
            { _, anio, mes, dia -> fijarFecha(LocalDate.of(anio, mes + 1, dia).toString()) },
            inicial.year,
            inicial.monthValue - 1,
            inicial.dayOfMonth,
        )
        dialogo.datePicker.maxDate = System.currentTimeMillis() // no se permiten fechas futuras
        dialogo.show()
    }

    private fun guardar() {
        val nombre = binding.campoNombre.text.toString().trim()
        val raza = binding.campoRaza.text.toString().trim().ifEmpty { null }
        val textoPeso = binding.campoPeso.text.toString().trim().replace(',', '.')
        val peso = if (textoPeso.isEmpty()) null else textoPeso.toDoubleOrNull()

        // Cada campo obligatorio vacío muestra "Este campo es obligatorio" (ERS RF05).
        binding.errorEspecie.visibility = View.GONE
        binding.errorNombre.visibility = View.GONE
        binding.errorSexo.visibility = View.GONE
        binding.errorPeso.visibility = View.GONE
        binding.textoError.visibility = View.GONE

        var hayErrores = false
        if (!editando && especie == null) {
            binding.errorEspecie.visibility = View.VISIBLE
            hayErrores = true
        }
        if (nombre.isEmpty()) {
            binding.errorNombre.visibility = View.VISIBLE
            hayErrores = true
        }
        if (sexo == null) {
            binding.errorSexo.visibility = View.VISIBLE
            hayErrores = true
        }
        if (textoPeso.isNotEmpty() && (peso == null || peso <= 0)) {
            binding.errorPeso.visibility = View.VISIBLE
            hayErrores = true
        }
        if (hayErrores) return

        val datos = MascotaPeticion(
            nombre = nombre,
            sexo = sexo!!,
            raza = raza,
            fechaNacimiento = fechaNacimiento,
            peso = peso,
            especie = if (editando) null else especie,
        )

        binding.botonGuardar.isEnabled = false

        lifecycleScope.launch {
            val resultado = if (editando) repositorio.editar(mascotaId, datos) else repositorio.crear(datos)
            when (resultado) {
                is Resultado.Exito -> {
                    // Con la mascota ya guardada, se sube (o se quita) la foto.
                    val id = resultado.dato.id
                    val bytes = fotoNueva
                    var fotoGuardada = true
                    if (bytes != null) {
                        fotoGuardada = repositorio.subirFoto(id, bytes) is Resultado.Exito
                    } else if (quitarFotoActual) {
                        repositorio.quitarFoto(id)
                    }
                    val mensaje = when {
                        !fotoGuardada -> R.string.foto_error_subida
                        editando -> R.string.mascota_actualizada
                        else -> R.string.mascota_registrada
                    }
                    Toast.makeText(this@MascotaFormActivity, mensaje, Toast.LENGTH_SHORT).show()
                    finish()
                }
                is Resultado.Fallo -> {
                    mostrarError(resultado.paraMostrar(this@MascotaFormActivity))
                    binding.botonGuardar.isEnabled = true
                }
            }
        }
    }

    private fun mostrarError(mensaje: String) {
        binding.textoError.text = mensaje
        binding.textoError.visibility = View.VISIBLE
    }

    companion object {
        const val EXTRA_ID = "mascota_id"
    }
}
