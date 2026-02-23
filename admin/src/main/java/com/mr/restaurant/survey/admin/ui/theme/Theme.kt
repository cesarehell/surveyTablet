package com.mr.restaurant.survey.admin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val AdminLightScheme = lightColorScheme(
	primary = AdminLightPrimary,
	onPrimary = AdminLightOnPrimary,
	secondary = AdminLightSecondary,
	onSecondary = AdminLightOnSecondary,
	background = AdminLightBackground,
	surface = AdminLightSurface,
	error = AdminLightError,
	outline = AdminLightOutline,
	primaryContainer = AdminLightAccentBlue.copy(alpha = 0.10f),
	secondaryContainer = AdminLightSecondary.copy(alpha = 0.22f),
	errorContainer = AdminLightAccentRed.copy(alpha = 0.18f)
)

private val AdminDarkScheme = darkColorScheme(
	primary = AdminDarkPrimary,
	onPrimary = AdminDarkOnPrimary,
	secondary = AdminDarkSecondary,
	onSecondary = AdminDarkOnSecondary,
	background = AdminDarkBackground,
	surface = AdminDarkSurface,
	error = AdminDarkError,
	outline = AdminDarkOutline,
	primaryContainer = AdminDarkAccentBlue.copy(alpha = 0.16f),
	secondaryContainer = AdminDarkSecondary.copy(alpha = 0.18f),
	errorContainer = AdminDarkAccentRed.copy(alpha = 0.16f)
)

private val AdminShapes = Shapes(
	small = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
	medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
	large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
)

@Composable
fun AdminTheme(
	darkTheme: Boolean = isSystemInDarkTheme(),
	content: @Composable () -> Unit
) {
	val colors = if (darkTheme) AdminDarkScheme else AdminLightScheme
	MaterialTheme(
		colorScheme = colors,
		shapes = AdminShapes,
		content = content
	)
}
