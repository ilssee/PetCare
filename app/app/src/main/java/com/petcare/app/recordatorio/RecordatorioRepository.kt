package com.petcare.app.recordatorio

import com.petcare.app.common.ApiClient
import com.petcare.app.common.Resultado
import com.petcare.app.common.llamarApi

/** Intermediario entre la pantalla de recordatorios y la API. */
class RecordatorioRepository(private val api: RecordatorioApi = ApiClient.crear(RecordatorioApi::class.java)) {
    suspend fun obtener(): Resultado<DiasRecordatorio> = llamarApi { api.obtener() }

    /** Si [dias] viene vacío, el servidor restablece todos los días. */
    suspend fun guardar(dias: List<String>): Resultado<GuardarDiasRespuesta> =
        llamarApi { api.guardar(GuardarDiasPeticion(dias)) }
}
