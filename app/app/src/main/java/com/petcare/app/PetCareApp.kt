package com.petcare.app

import android.app.Application
import com.petcare.app.common.SessionManager

/** Se ejecuta una sola vez al abrir la app. Aquí se prepara lo que usa toda la aplicación. */
class PetCareApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionManager.iniciar(this)
    }
}
