package com.petcare.app.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.petcare.app.databinding.FragmentPlaceholderBinding

/** Sección que todavía no está construida. Se irá reemplazando módulo por módulo. */
class PlaceholderFragment : Fragment() {

    private var _binding: FragmentPlaceholderBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, contenedor: ViewGroup?, estado: Bundle?): View {
        _binding = FragmentPlaceholderBinding.inflate(inflater, contenedor, false)
        binding.titulo.text = requireArguments().getString(ARG_TITULO)
        binding.root.respetarBarraDeEstado()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TITULO = "titulo"
        fun nuevo(titulo: String) = PlaceholderFragment().apply { arguments = bundleOf(ARG_TITULO to titulo) }
    }
}
