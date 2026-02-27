package com.mr.restaurant.survey.admin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
	Surface(
		color = MaterialTheme.colorScheme.primary,
		contentColor = MaterialTheme.colorScheme.onPrimary,
		shadowElevation = 6.dp
	) {
		Row(
			Modifier
				.fillMaxWidth()
				.padding(horizontal = 12.dp, vertical = 14.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			Card(
				colors = CardDefaults.cardColors(
					containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f)
				)
			) {
				TextButton(onClick = onBack) { Text("←", color = MaterialTheme.colorScheme.onPrimary) }
			}
			Spacer(Modifier.width(8.dp))
			Column(Modifier.weight(1f)) {
				Text(
					title,
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.SemiBold
				)
				if (!subtitle.isNullOrBlank()) {
					Text(
						subtitle,
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
					)
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

@Composable
fun ErrorWithRetry(
	message: String,
	onRetry: () -> Unit,
	modifier: Modifier = Modifier
) {
	Column(
		modifier = modifier.fillMaxWidth(),
		verticalArrangement = Arrangement.spacedBy(8.dp)
	) {
		ErrorBanner(message = message)
		Button(onClick = onRetry) {
			Text("Reintentar")
		}
	}
}

@Composable
fun EmptyState(
	message: String,
	modifier: Modifier = Modifier
) {
	Column(
		modifier = modifier
			.fillMaxSize()
			.padding(24.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		Text(message, style = MaterialTheme.typography.bodyLarge)
	}
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> FilterChipWrap(
	items: List<T>,
	selectedItem: T,
	onSelect: (T) -> Unit,
	labelContent: @Composable (T) -> Unit,
	modifier: Modifier = Modifier,
	enabled: (T) -> Boolean = { true }
) {
	FlowRow(
		modifier = modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.spacedBy(8.dp),
		verticalArrangement = Arrangement.spacedBy(8.dp)
	) {
		items.forEach { item ->
			FilterChip(
				selected = item == selectedItem,
				onClick = { onSelect(item) },
				enabled = enabled(item),
				label = { labelContent(item) }
			)
		}
	}
}

@Composable
fun DialogConfirmButton(
	text: String,
	enabled: Boolean = true,
	onClick: () -> Unit
) {
	Button(onClick = onClick, enabled = enabled) {
		Text(text)
	}
}

@Composable
fun DialogCancelButton(
	text: String = "Cancelar",
	onClick: () -> Unit
) {
	TextButton(onClick = onClick) {
		Text(text)
	}
}

@Composable
fun ReadOnlyStatusChip(
	active: Boolean,
	activeLabel: String = "Activa",
	inactiveLabel: String = "Inactiva"
) {
	FilterChip(
		selected = active,
		onClick = {},
		enabled = false,
		label = { Text(if (active) activeLabel else inactiveLabel) }
	)
}
