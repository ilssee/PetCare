package com.petcare.app.mascota

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.petcare.app.common.Resultado
import com.petcare.app.R
import com.petcare.app.common.configurar
import com.petcare.app.common.paraMostrar
import com.petcare.app.common.respetarBarraDeEstado
import com.petcare.app.databinding.FragmentMascotasBinding
import kotlinx.coroutines.launch

/** Sección "Mis mascotas": lista de mascotas del usuario y botón para registrar una nueva. */
class MascotasFragment : Fragment() {

    private var _binding: FragmentMascotasBinding? = null
    private val binding get() = _binding!!
    private val repositorio = MascotaRepository()

    private val adaptador = MascotaAdapter { mascota ->
        startActivity(
            Intent(requireContext(), MascotaPerfilActivity::class.java)
                .putExtra(MascotaPerfilActivity.EXTRA_ID, mascota.id),
        )
    }

    override fun onCreateView(inflater: LayoutInflater, contenedor: ViewGroup?, estado: Bundle?): View {
        _binding = FragmentMascotasBinding.inflate(inflater, contenedor, false)
        binding.lista.layoutManager = LinearLayoutManager(requireContext())
        binding.lista.adapter = adaptador
        val abrirFormulario = { startActivity(Intent(requireContext(), MascotaFormActivity::class.java)) }
        binding.botonNuevaMascota.setOnClickListener { abrirFormulario() }
        binding.vacio.configurar(R.drawable.ic_huella, R.string.mascotas_vacio_titulo, R.string.mascotas_vacio_mensaje, alTocarBoton = abrirFormulario)
        binding.root.respetarBarraDeEstado()
        return binding.root
    }

    // Se recarga al volver de crear, editar o eliminar una mascota.
    override fun onResume() {
        super.onResume()
        cargar()
    }

    private fun cargar() {
        viewLifecycleOwner.lifecycleScope.launch {
            when (val resultado = repositorio.listar()) {
                is Resultado.Exito -> {
                    adaptador.submitList(resultado.dato)
                    // Sin mascotas: se muestra la invitación a registrar la primera (y se oculta el botón flotante).
                    val sinMascotas = resultado.dato.isEmpty()
                    binding.vacio.root.visibility = if (sinMascotas) View.VISIBLE else View.GONE
                    binding.botonNuevaMascota.visibility = if (sinMascotas) View.GONE else View.VISIBLE
                    binding.textoError.visibility = View.GONE
                }
                is Resultado.Fallo -> {
                    binding.textoError.text = resultado.paraMostrar(requireContext())
                    binding.textoError.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
