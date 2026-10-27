package com.petcare.app.cronograma

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.petcare.app.R
import com.petcare.app.common.Resultado
import com.petcare.app.common.SessionManager
import com.petcare.app.common.configurar
import com.petcare.app.common.respetarBarraDeEstado
import com.petcare.app.databinding.FragmentCronogramaBinding
import com.petcare.app.databinding.ItemDiaSemanaBinding
import com.petcare.app.mascota.MascotaFormActivity
import com.petcare.app.mascota.MascotaRepository
import com.petcare.app.mascota.configurarSiNoHayMascotas
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Sección Cronograma. Por ahora muestra la cabecera, la semana actual y el estado "sin mascotas". */
class CronogramaFragment : Fragment() {

    private var _binding: FragmentCronogramaBinding? = null
    private val binding get() = _binding!!
    private val repositorio = MascotaRepository()

    override fun onCreateView(inflater: LayoutInflater, contenedor: ViewGroup?, estado: Bundle?): View {
        _binding = FragmentCronogramaBinding.inflate(inflater, contenedor, false)
        binding.cabecera.respetarBarraDeEstado()

        val nombre = SessionManager.nombre?.trim()?.substringBefore(' ').orEmpty()
        binding.saludo.text = getString(R.string.cronograma_saludo, nombre)

        // El resumen de arriba solo tiene sentido cuando hay cuidados; sin ninguno, ya lo dice el mensaje del centro.
        val hayCuidados = false // se calculará cuando exista el módulo de cuidados
        binding.tarjetaResumen.visibility = if (hayCuidados) View.VISIBLE else View.GONE

        val hoy = LocalDate.now()
        binding.mes.text = hoy.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale("es", "PE"))).replace(".", "")
        mostrarSemana(hoy)

        binding.botonBuscar.setOnClickListener {
            Toast.makeText(requireContext(), R.string.proximamente, Toast.LENGTH_SHORT).show()
        }
        return binding.root
    }

    /** Dibuja los siete días de la semana actual (lunes a domingo) y resalta el de hoy. */
    private fun mostrarSemana(hoy: LocalDate) {
        val letras = listOf("L", "M", "X", "J", "V", "S", "D")
        val lunes = hoy.with(DayOfWeek.MONDAY)
        for (i in 0..6) {
            val dia = lunes.plusDays(i.toLong())
            val item = ItemDiaSemanaBinding.inflate(layoutInflater, binding.semana, false)
            item.letra.text = letras[i]
            item.numero.text = dia.dayOfMonth.toString()
            item.root.isSelected = dia == hoy
            binding.semana.addView(item.root)
        }
    }

    // Se vuelve a revisar al regresar de registrar una mascota.
    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch {
            val resultado = repositorio.listar()
            if (resultado is Resultado.Exito) {
                // Sin mascotas activas: aviso de mascotas. Con mascotas: aún no hay ningún cuidado,
                // y el botón lleva a las recomendaciones (pantalla pendiente: por ahora avisa "Próximamente").
                val faltanMascotas = binding.vacio.configurarSiNoHayMascotas(resultado.dato) {
                    startActivity(Intent(requireContext(), MascotaFormActivity::class.java))
                }
                if (!faltanMascotas) {
                    binding.vacio.configurar(
                        R.drawable.ic_corazon,
                        R.string.sin_cuidados_titulo,
                        R.string.sin_cuidados_mensaje,
                        R.string.boton_ver_recomendados,
                    ) {
                        Toast.makeText(requireContext(), R.string.proximamente, Toast.LENGTH_SHORT).show()
                    }
                }
                binding.vacio.root.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
