package com.petcare.app.usuario

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.petcare.app.R
import com.petcare.app.common.SessionManager
import com.petcare.app.databinding.FragmentPerfilBinding

/** Sección Perfil (provisional): muestra el nombre y permite cerrar sesión. */
class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, contenedor: ViewGroup?, estado: Bundle?): View {
        _binding = FragmentPerfilBinding.inflate(inflater, contenedor, false)
        binding.textoNombre.text = SessionManager.nombre ?: ""
        binding.botonCerrarSesion.setOnClickListener { confirmarCierreDeSesion() }
        binding.tituloPerfil.text = getString(R.string.nav_perfil)
        return binding.root
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
