package com.petcare.app.resena

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.petcare.app.databinding.ItemResenaBinding
import com.petcare.app.mapa.FormatosMapa
import com.petcare.app.mapa.ResenaRespuesta

/** Una reseña como se ve en el perfil de la veterinaria y en la lista completa. */
object ResenaFila {

    /**
     * Crea la fila de una reseña. [alEditar] y [alEliminar] se pasan solo donde se permiten esas acciones
     * (la lista completa); aun así, los botones solo aparecen en las reseñas del propio usuario (RF46).
     */
    fun crear(
        padre: ViewGroup,
        resena: ResenaRespuesta,
        alEditar: ((ResenaRespuesta) -> Unit)? = null,
        alEliminar: ((ResenaRespuesta) -> Unit)? = null,
    ): View {
        val fila = ItemResenaBinding.inflate(LayoutInflater.from(padre.context), padre, false)
        fila.inicial.text = resena.autor.trim().take(1).uppercase()
        fila.autor.text = resena.autor
        fila.fecha.text = FormatosMapa.fechaCorta(resena.fecha)
        FormatosMapa.pintarEstrellas(fila.estrellas, resena.calificacion, 16)
        fila.comentario.isVisible = !resena.comentario.isNullOrBlank()
        fila.comentario.text = resena.comentario.orEmpty()

        val conAcciones = resena.esMia && (alEditar != null || alEliminar != null)
        fila.acciones.isVisible = conAcciones
        if (conAcciones) {
            fila.editar.setOnClickListener { alEditar?.invoke(resena) }
            fila.eliminar.setOnClickListener { alEliminar?.invoke(resena) }
        }
        return fila.root
    }
}
