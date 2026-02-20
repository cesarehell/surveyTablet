package com.mr.restaurant.survey.admin.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SimpleTopBar(
	title: String,
	subtitle: String? = null,
	onBack: () -> Unit
) {
	Surface(shadowElevation = 2.dp) {
		Row(
			Modifier
				.fillMaxWidth()
				.padding(horizontal = 12.dp, vertical = 12.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			TextButton(onClick = onBack) { Text("←") }
			Spacer(Modifier.width(8.dp))
			Column(Modifier.weight(1f)) {
				Text(
					title,
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.SemiBold
				)
				if (!subtitle.isNullOrBlank()) {
					Text(subtitle, style = MaterialTheme.typography.bodySmall)
				}
			}
		}
	}
}

@Composable
fun ErrorBanner(message: String) {
	Surface(
		color = MaterialTheme.colorScheme.errorContainer,
		contentColor = MaterialTheme.colorScheme.onErrorContainer,
		tonalElevation = 2.dp,
		shape = MaterialTheme.shapes.medium
	) {
		Text(
			text = message,
			modifier = Modifier.padding(12.dp),
			style = MaterialTheme.typography.bodyMedium
		)
	}
}