package com.petcare.app.mascota

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.petcare.app.R
import com.petcare.app.databinding.ItemMascotaBinding

/** Muestra cada mascota como una tarjeta del color de su especie. */
class MascotaAdapter(
    private val alTocar: (MascotaRespuesta) -> Unit,
) : ListAdapter<MascotaRespuesta, MascotaAdapter.Tarjeta>(Comparador) {

    class Tarjeta(val binding: ItemMascotaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(padre: ViewGroup, tipo: Int) =
        Tarjeta(ItemMascotaBinding.inflate(LayoutInflater.from(padre.context), padre, false))

    override fun onBindViewHolder(tarjeta: Tarjeta, posicion: Int) {
        val mascota = getItem(posicion)
        val b = tarjeta.binding
        val color = ContextCompat.getColor(b.root.context, Especies.color(mascota.especie))

        b.tarjeta.backgroundTintList = ColorStateList.valueOf(color)
        b.nombre.text = mascota.nombre
        // Raza y edad son opcionales: si faltan, no se muestran.
        b.raza.text = mascota.raza
        b.raza.visibility = if (mascota.raza.isNullOrBlank()) View.GONE else View.VISIBLE
        b.edad.text = mascota.edadTexto
        b.edad.visibility = if (mascota.edadTexto == null) View.GONE else View.VISIBLE
        b.sexo.text = mascota.sexo.conPrimeraMayuscula()
        b.inactiva.visibility = if (mascota.estaInactiva) View.VISIBLE else View.GONE
        b.cuidados.text = b.root.resources.getQuantityString(
            R.plurals.mascota_total_cuidados, mascota.totalCuidados, mascota.totalCuidados,
        )
        b.ilustracion.mostrarMascota(mascota)
        b.tarjeta.setOnClickListener { alTocar(mascota) }
    }

    private object Comparador : DiffUtil.ItemCallback<MascotaRespuesta>() {
        override fun areItemsTheSame(a: MascotaRespuesta, b: MascotaRespuesta) = a.id == b.id
        override fun areContentsTheSame(a: MascotaRespuesta, b: MascotaRespuesta) = a == b
    }
}
