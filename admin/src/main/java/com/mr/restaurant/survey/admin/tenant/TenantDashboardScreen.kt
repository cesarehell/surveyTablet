package com.mr.restaurant.survey.admin.tenant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mr.restaurant.survey.admin.R
import androidx.compose.ui.res.stringResource

@Composable
fun TenantDashboardScreen(
	tenantId: String,
	onBack: () -> Unit,
	onMetrics: () -> Unit,
	onLocations: () -> Unit,
	onDevices: () -> Unit,
	onTemplates: () -> Unit,
	onThresholds: () -> Unit,
	onCoupons: () -> Unit,
	onAlerts: () -> Unit,
	onPushDebug: () -> Unit,
) {
	LazyColumn(
		modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.background)
			.padding(horizontal = 18.dp),
		verticalArrangement = Arrangement.spacedBy(14.dp)
	) {
		item { Spacer(Modifier.height(8.dp)) }
		item {
			TextButton(onClick = onBack, modifier = Modifier.padding(start = 0.dp)) {
				Text("← Franquicias")
			}
		}
		item {
			Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
				Text(
					text = "Operación",
					style = MaterialTheme.typography.headlineLarge,
					fontWeight = FontWeight.ExtraBold
				)
				Text(
					text = tenantId,
					style = MaterialTheme.typography.titleMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis
				)
			}
		}

		item {
			AlertBannerCard(onAlerts = onAlerts)
		}

		item {
			PrimaryActionCard(
				title = stringResource(R.string.dashboard_action_metrics),
				subtitle = stringResource(R.string.dashboard_action_metrics_subtitle),
				badge = "ME",
				onClick = onMetrics
			)
		}

		item {
			Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
				SquareActionCard(
					title = stringResource(R.string.dashboard_action_surveys),
					subtitle = stringResource(R.string.dashboard_action_surveys_subtitle),
					badge = "EN",
					onClick = onTemplates,
					modifier = Modifier.weight(1f)
				)
				SquareActionCard(
					title = "Reglas",
					subtitle = "Alertas automáticas",
					badge = "RG",
					onClick = onThresholds,
					modifier = Modifier.weight(1f)
				)
			}
		}

		item {
			Text(
				text = "Configuración",
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.SemiBold,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}

		item {
			Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
				SettingRowCard(
					title = stringResource(R.string.dashboard_action_coupons),
					subtitle = stringResource(R.string.dashboard_action_coupons_subtitle),
					badge = "CP",
					buttonText = "Configurar",
					onClick = onCoupons,
					highlight = true
				)
				SettingRowCard(
					title = "Sucursales",
					subtitle = "Gestión de locales",
					badge = "SU",
					buttonText = "Configurar",
					onClick = onLocations
				)
				SettingRowCard(
					title = "Tablets",
					subtitle = "Vinculación de equipos",
					badge = "TB",
					buttonText = "Configurar",
					onClick = onDevices
				)
			}
		}

		if (false) {
			item { TextButton(onClick = onPushDebug) { Text("Push Debug") } }
		}

		item { Spacer(Modifier.height(20.dp)) }
	}
}

@Composable
private fun AlertBannerCard(onAlerts: () -> Unit) {
	Card(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(onClick = onAlerts),
		shape = RoundedCornerShape(24.dp),
		colors = CardDefaults.cardColors(
			containerColor = Color(0xFFDDF3E6)
		),
		border = CardDefaults.outlinedCardBorder().copy(
			brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF9BE5B5))
		)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(12.dp)
		) {
			BadgeCircle(label = "!", strong = true)
			Column(modifier = Modifier.weight(1f)) {
				Text(
					text = "Alertas activas",
					style = MaterialTheme.typography.labelLarge,
					fontWeight = FontWeight.Bold,
					color = Color(0xFF10B95A)
				)
				Text(
					text = "Revisar pendientes y atención",
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.SemiBold
				)
			}
			Surface(
				shape = RoundedCornerShape(999.dp),
				color = MaterialTheme.colorScheme.surface
			) {
				Text(
					text = "Ver ahora",
					modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
					fontWeight = FontWeight.SemiBold,
					color = Color(0xFF10B95A)
				)
			}
		}
	}
}

@Composable
private fun PrimaryActionCard(
	title: String,
	subtitle: String,
	badge: String,
	onClick: () -> Unit
) {
	Card(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(onClick = onClick),
		shape = RoundedCornerShape(24.dp),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
		elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(18.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(14.dp)
		) {
			BadgeCircle(label = badge, strong = false)
			Column(modifier = Modifier.weight(1f)) {
				Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
				Text(subtitle, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
			}
			Icon(
				imageVector = Icons.Outlined.ChevronRight,
				contentDescription = null,
				tint = MaterialTheme.colorScheme.outline
			)
		}
	}
}

@Composable
private fun SquareActionCard(
	title: String,
	subtitle: String,
	badge: String,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
) {
	Card(
		modifier = modifier
			.clickable(onClick = onClick),
		shape = RoundedCornerShape(24.dp),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
		elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.height(170.dp)
				.padding(16.dp),
			verticalArrangement = Arrangement.SpaceBetween
		) {
			BadgeCircle(label = badge, strong = false)
			Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
				Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
				Text(subtitle, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
			}
		}
	}
}

@Composable
private fun SettingRowCard(
	title: String,
	subtitle: String,
	badge: String,
	buttonText: String,
	onClick: () -> Unit,
	highlight: Boolean = false,
) {
	Card(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(onClick = onClick),
		shape = RoundedCornerShape(22.dp),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
		elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(14.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(12.dp)
		) {
			BadgeCircle(label = badge, strong = highlight)
			Column(modifier = Modifier.weight(1f)) {
				Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
				Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
			}
			Surface(
				shape = RoundedCornerShape(999.dp),
				color = if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
				else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
			) {
				Text(
					text = buttonText,
					modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
					fontWeight = FontWeight.SemiBold,
					color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
		}
	}
}

@Composable
private fun BadgeCircle(label: String, strong: Boolean) {
	Surface(
		shape = RoundedCornerShape(18.dp),
		color = if (strong) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
		else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
	) {
		Box(
			modifier = Modifier.size(if (strong) 52.dp else 50.dp),
			contentAlignment = Alignment.Center
		) {
			Text(
				text = label,
				fontWeight = FontWeight.Bold,
				color = MaterialTheme.colorScheme.primary,
				style = MaterialTheme.typography.titleMedium
			)
		}
	}
}
