package com.mr.restaurant.survey.admin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AdminLightScheme = lightColorScheme(
	primary = AdminLightPrimary,
	onPrimary = AdminLightOnPrimary,
	secondary = AdminLightSecondary,
	onSecondary = AdminLightOnSecondary,
	background = AdminLightBackground,
	surface = AdminLightSurface,
	error = AdminLightError
)

private val AdminDarkScheme = darkColorScheme(
	primary = AdminDarkPrimary,
	onPrimary = AdminDarkOnPrimary,
	secondary = AdminDarkSecondary,
	onSecondary = AdminDarkOnSecondary,
	background = AdminDarkBackground,
	surface = AdminDarkSurface,
	error = AdminDarkError
)

@Composable
fun AdminTheme(
	darkTheme: Boolean = isSystemInDarkTheme(),
	content: @Composable () -> Unit
) {
	val colors = if (darkTheme) AdminDarkScheme else AdminLightScheme
	MaterialTheme(
		colorScheme = colors,
		content = content
	)
}
