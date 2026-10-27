package com.petcare.app.usuario

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.petcare.app.R
import com.petcare.app.common.Resultado
import com.petcare.app.common.SessionManager
import com.petcare.app.common.respetarBarraDeEstado
import com.petcare.app.databinding.FragmentPerfilBinding
import com.petcare.app.mascota.MascotaRepository
import kotlinx.coroutines.launch

/** Sección Perfil: datos del usuario, accesos a editar el perfil y cierre de sesión (con confirmación). */
class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!
    private val usuarios = UsuarioRepository()
    private val mascotas = MascotaRepository()

    override fun onCreateView(inflater: LayoutInflater, contenedor: ViewGroup?, estado: Bundle?): View {
        _binding = FragmentPerfilBinding.inflate(inflater, contenedor, false)
        binding.root.respetarBarraDeEstado()

        mostrarNombre(SessionManager.nombre.orEmpty())
        binding.filaEditar.setOnClickListener {
            startActivity(Intent(requireContext(), EditarPerfilActivity::class.java))
        }
        // Recordatorios y avisos de cercanía pertenecen a módulos que aún no están construidos.
        val proximamente = View.OnClickListener {
            Toast.makeText(requireContext(), R.string.proximamente, Toast.LENGTH_SHORT).show()
        }
        binding.filaRecordatorios.setOnClickListener(proximamente)
        binding.filaAvisos.setOnClickListener(proximamente)
        binding.filaCerrarSesion.setOnClickListener { confirmarCierreDeSesion() }
        return binding.root
    }

    // Se recarga al volver de editar el perfil o de registrar mascotas.
    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch {
            (usuarios.obtenerPerfil() as? Resultado.Exito)?.let {
                mostrarNombre(it.dato.nombre)
                binding.textoCorreo.text = it.dato.correo
            }
            (mascotas.listar() as? Resultado.Exito)?.let { binding.statMascotas.text = it.dato.size.toString() }
        }
    }

    private fun mostrarNombre(nombre: String) {
        binding.textoNombre.text = nombre
        binding.iniciales.text = nombre.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            .take(2).joinToString("") { it.first().uppercase() }
    }

    /** RF03: pide confirmación; si el usuario cancela, la sesión sigue activa. */
    private fun confirmarCierreDeSesion() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.cerrar_sesion_titulo)
            .setMessage(R.string.cerrar_sesion_mensaje)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.inicio_cerrar_sesion) { _, _ ->
                SessionManager.cerrar()
                startActivity(
                    Intent(requireContext(), LoginActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                )
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
