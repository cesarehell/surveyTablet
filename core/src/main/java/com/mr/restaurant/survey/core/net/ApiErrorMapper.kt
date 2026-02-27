package com.mr.restaurant.survey.core.net

import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

object ApiErrorMapper {

	private val json = Json {
		ignoreUnknownKeys = true
		explicitNulls = false
	}

	fun message(t: Throwable): String = when (t) {
		is HttpException -> httpMessage(t)
		is IOException -> "Sin conexión o el servidor no responde"
		else -> t.message ?: "Error inesperado"
	}

	private fun httpMessage(e: HttpException): String {
		val code = e.code()
		val raw = runCatching { e.response()?.errorBody()?.string() }.getOrNull()

		val dto = raw?.let {
			runCatching { json.decodeFromString(ApiErrorDto.serializer(), it) }.getOrNull()
		}

		val msg = dto?.message ?: dto?.error
		if (!msg.isNullOrBlank()) return normalizeBusinessMessage(msg)

		return when (code) {
			400 -> "Solicitud inválida (400)"
			401 -> "No autorizado (401)"
			403 -> "Prohibido (403)"
			404 -> "No encontrado (404)"
			else -> if (code in 500..599) "Error del servidor ($code)" else "Error HTTP ($code)"
		}
	}

	private fun normalizeBusinessMessage(message: String): String {
		val m = message.trim()
		val lower = m.lowercase()

		return when {
			lower.contains("email requerido para aceptar promociones") ->
				"Si activas promociones por correo, agrega un correo válido."

			lower.contains("email requerido") ->
				"Para emitir el cupón necesitamos un correo. Puedes continuar sin cupón o capturar correo."

			lower.contains("ya emitido") && lower.contains("30") ->
				"Este correo ya recibió un cupón recientemente."

			else -> m
		}
	}
}
