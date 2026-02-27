package com.mr.restaurant.survey.admin.validation

object AdminInputValidator {
	private const val MAX_NAME_LEN = 120

	fun normalizeName(raw: String): String = raw.trim().replace(Regex("\\s+"), " ")

	fun validateName(raw: String): String? {
		val normalized = normalizeName(raw)
		if (normalized.isBlank()) return "El nombre es obligatorio"
		if (normalized.length > MAX_NAME_LEN) return "El nombre es demasiado largo"
		if (normalized.any { it.isISOControl() }) return "El nombre contiene caracteres no válidos"
		if (containsSuspiciousPattern(normalized)) return "El nombre contiene caracteres no válidos"
		return null
	}

	fun sanitizeSingleLineInput(raw: String, maxLen: Int = MAX_NAME_LEN): String {
		val withoutControls = raw.filterNot { it.isISOControl() }
		return withoutControls.take(maxLen)
	}

	private fun containsSuspiciousPattern(value: String): Boolean {
		if (value.contains(';')) return true
		if (value.contains("/*") || value.contains("*/") || value.contains("--")) return true
		return value.any { it == '<' || it == '>' || it == '`' }
	}
}

