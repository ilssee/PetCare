package com.petcare.app.common

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat

/** Ubicación del celular. El permiso lo pide la pantalla que lo necesita; aquí solo se lee. */
object UbicacionActual {

    fun tienePermiso(contexto: Context) =
        permitido(contexto, Manifest.permission.ACCESS_FINE_LOCATION) ||
            permitido(contexto, Manifest.permission.ACCESS_COARSE_LOCATION)

    /** Los dos permisos que se piden juntos: el usuario puede dar solo el aproximado. */
    val PERMISOS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    private fun permitido(contexto: Context, permiso: String) =
        ContextCompat.checkSelfPermission(contexto, permiso) == PackageManager.PERMISSION_GRANTED

    /**
     * Obtiene la ubicación actual y la entrega a [alTerminar], o null si no se pudo (sin permiso, GPS apagado o
     * sin señal). Con solo el permiso aproximado no se puede usar el GPS y se usa la ubicación por red.
     */
    @SuppressLint("MissingPermission") // el permiso se comprueba antes de leer
    fun obtener(contexto: Context, alTerminar: (Location?) -> Unit) {
        if (!tienePermiso(contexto)) {
            alTerminar(null)
            return
        }
        val gestor = contexto.getSystemService(LocationManager::class.java)
        val exacta = permitido(contexto, Manifest.permission.ACCESS_FINE_LOCATION)
        val proveedores =
            if (exacta) listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER) else listOf(LocationManager.NETWORK_PROVIDER)
        val proveedor = proveedores.firstOrNull { gestor.isProviderEnabled(it) }
        if (proveedor == null) {
            alTerminar(null)
            return
        }
        try {
            gestor.getCurrentLocation(proveedor, null, ContextCompat.getMainExecutor(contexto)) { lugar -> alTerminar(lugar) }
        } catch (e: SecurityException) {
            alTerminar(null)
        }
    }
}
