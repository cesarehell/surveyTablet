package com.mr.restaurant.survey.net.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = QuestionTypeSerializer::class)
enum class QuestionType {
	LIKERT_5,
	LIKERT_10,
	YES_NO,
	SINGLE,
	MULTI,
	TEXT
}

object QuestionTypeSerializer : KSerializer<QuestionType> {
	override val descriptor: SerialDescriptor =
		PrimitiveSerialDescriptor("QuestionType", PrimitiveKind.STRING)

	override fun deserialize(decoder: Decoder): QuestionType {
		return when (decoder.decodeString().trim().uppercase()) {
			"LIKERT_5", "RATING" -> QuestionType.LIKERT_5
			"LIKERT_10" -> QuestionType.LIKERT_10
			"YESNO", "YES_NO" -> QuestionType.YES_NO
			"SINGLE", "SINGLE_CHOICE" -> QuestionType.SINGLE
			"MULTI", "MULTI_CHOICE" -> QuestionType.MULTI
			"TEXT", "OPEN_TEXT" -> QuestionType.TEXT
			else -> throw IllegalArgumentException("QuestionType no soportado")
		}
	}

	override fun serialize(encoder: Encoder, value: QuestionType) {
		val wireValue = when (value) {
			QuestionType.LIKERT_5 -> "LIKERT_5"
			QuestionType.LIKERT_10 -> "LIKERT_10"
			QuestionType.YES_NO -> "YESNO"
			QuestionType.SINGLE -> "SINGLE"
			QuestionType.MULTI -> "MULTI"
			QuestionType.TEXT -> "TEXT"
		}
		encoder.encodeString(wireValue)
	}
}
