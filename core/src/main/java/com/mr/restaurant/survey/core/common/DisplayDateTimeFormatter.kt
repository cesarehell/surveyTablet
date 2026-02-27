package com.mr.restaurant.survey.core.common

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DisplayDateTimeFormatter {
	private val locale = Locale("es", "MX")
	private val fmtDate = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
	private val fmtDayShort = DateTimeFormatter.ofPattern("d MMM", locale)
	private val fmtDateTime = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", locale)

	fun humanDayShort(value: String?): String {
		if (value.isNullOrBlank()) return "-"
		return runCatching { LocalDate.parse(value).format(fmtDayShort) }
			.getOrElse { value.takeLast(5) }
	}

	fun humanDate(value: String?): String {
		if (value.isNullOrBlank()) return "-"
		return parseBestDateTime(value)?.toLocalDate()?.format(fmtDate)
			?: runCatching { LocalDate.parse(value).format(fmtDate) }.getOrElse { value }
	}

	fun humanDateTime(value: String?): String {
		if (value.isNullOrBlank()) return "-"
		return parseBestDateTime(value)?.format(fmtDateTime)?.lowercase(locale) ?: humanDate(value)
	}

	private fun parseBestDateTime(value: String): LocalDateTime? {
		return runCatching {
			Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDateTime()
		}.getOrNull()
			?: runCatching {
				OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
			}.getOrNull()
			?: runCatching { LocalDateTime.parse(value) }.getOrNull()
	}
}
