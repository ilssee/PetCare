package com.petcare.app.cuidado

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import android.widget.Toast
import com.petcare.app.R
import com.petcare.app.common.Resultado
import com.petcare.app.common.configurar
import com.petcare.app.common.respetarBarraDeEstado
import com.petcare.app.databinding.FragmentCuidadosBinding
import com.petcare.app.mascota.MascotaFormActivity
import com.petcare.app.mascota.MascotaRepository
import com.petcare.app.mascota.configurarSiNoHayMascotas
import kotlinx.coroutines.launch

/** Sección Cuidados. Por ahora solo muestra la invitación cuando no hay mascotas activas. */
class CuidadosFragment : Fragment() {

    private var _binding: FragmentCuidadosBinding? = null
    private val binding get() = _binding!!
    private val repositorio = MascotaRepository()

    override fun onCreateView(inflater: LayoutInflater, contenedor: ViewGroup?, estado: Bundle?): View {
        _binding = FragmentCuidadosBinding.inflate(inflater, contenedor, false)
        binding.root.respetarBarraDeEstado()
        return binding.root
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
