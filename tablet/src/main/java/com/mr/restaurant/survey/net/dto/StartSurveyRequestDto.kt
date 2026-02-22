import kotlinx.serialization.Serializable

@Serializable
data class StartSurveyRequestDto(
	val tenantId: String,
	val templateId: String,
	val locationId: String? = null,
	val tableNo: String? = null,
	val waiterName: String? = null
)