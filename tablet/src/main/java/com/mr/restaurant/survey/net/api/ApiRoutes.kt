package com.mr.restaurant.survey.net.api

object ApiRoutes {
	private const val V1 = "v1"
	const val TEMPLATES = "$V1/templates"
	const val CURRENT_TEMPLATE = "$TEMPLATES/current"
	const val CURRENT_TEMPLATE_FULL = "$TEMPLATES/current/full"
	const val TEMPLATE_BY_ID = "$TEMPLATES/{id}"
	const val REGISTER_DEVICE = "$V1/devices/register"
	const val QUESTIONS_OPTIONS = "$V1/questions/{id}/options"
	const val SURVEYS = "$V1/surveys"
	const val START_SURVEY = "$SURVEYS/start"
	const val SURVEY_ANSWERS = "$SURVEYS/{instId}/answers"
	const val SUBMIT_SURVEY = "$SURVEYS/{instId}/submit"
	const val ALERTS = "$V1/alerts"
}