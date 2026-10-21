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
import com.petcare.app.common.paraMostrar
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
        binding.botonNuevaMascota.setOnClickListener {
            startActivity(Intent(requireContext(), MascotaFormActivity::class.java))
        }
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
                    binding.vacio.visibility = if (resultado.dato.isEmpty()) View.VISIBLE else View.GONE
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
