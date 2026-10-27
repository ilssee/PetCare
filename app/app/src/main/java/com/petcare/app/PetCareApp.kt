package com.petcare.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.petcare.app.common.SessionManager

/** Se ejecuta una sola vez al abrir la app. Aquí se prepara lo que usa toda la aplicación. */
class PetCareApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // El diseño de PetCare es claro: se ignora el modo oscuro del sistema.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        SessionManager.iniciar(this)
    }
}
