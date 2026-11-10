package com.petcare.app.cercania

import com.petcare.app.common.ApiClient
import com.petcare.app.common.Resultado
import com.petcare.app.common.llamarApi

/** Intermediario entre la pantalla de avisos cerca de ti y la API. */
class CercaniaRepository(private val api: CercaniaApi = ApiClient.crear(CercaniaApi::class.java)) {
    suspend fun obtener(): Resultado<ConfiguracionCercania> = llamarApi { api.obtener() }

    suspend fun guardar(configuracion: ConfiguracionCercania): Resultado<ConfiguracionCercania> =
        llamarApi { api.guardar(configuracion) }
}
