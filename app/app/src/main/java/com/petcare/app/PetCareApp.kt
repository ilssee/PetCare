package com.petcare.app

import android.app.Application
import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import com.petcare.app.common.ApiClient
import com.petcare.app.common.SessionManager
import com.petcare.app.notificacion.Notificaciones
import com.petcare.app.usuario.LoginActivity

/** Se ejecuta una sola vez al abrir la app. Aquí se prepara lo que usa toda la aplicación. */
class PetCareApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // El diseño de PetCare es claro: se ignora el modo oscuro del sistema.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        SessionManager.iniciar(this)
        Notificaciones.crearCanal(this)
        // Si el servidor invalida la sesión (p. ej. cambio de contraseña en otro dispositivo), vuelve al login.
        ApiClient.alInvalidarseSesion = {
            startActivity(
                Intent(this, LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            )
        }
    }
}
