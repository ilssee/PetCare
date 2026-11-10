package com.petcare.app.recordatorio

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

/** Rutas de la API de recordatorios (ver backend/src/modules/recordatorio). Exigen sesión. */
interface RecordatorioApi {
    @GET("recordatorios")
    suspend fun obtener(): Response<DiasRecordatorio>

    @PUT("recordatorios")
    suspend fun guardar(@Body datos: GuardarDiasPeticion): Response<GuardarDiasRespuesta>
}
