package com.mr.restaurant.survey.admin.template

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mr.restaurant.survey.admin.ui.AdminUiEvent
import com.mr.restaurant.survey.admin.ui.EmptyState
import com.mr.restaurant.survey.admin.ui.ErrorWithRetry
import com.mr.restaurant.survey.core.template.dto.SurveyTemplateDto
import com.mr.restaurant.survey.core.template.dto.TemplateStatus

@Composable
fun TenantTemplateScreen(
	tenantId: String,
	onBack: () -> Unit,
	onCreate: () -> Unit,
	onOpenTemplate: (String) -> Unit,
	vm: TemplateViewModel = hiltViewModel()
) {
	val st by vm.state.collectAsState()
	val snackbarHostState = remember { SnackbarHostState() }

	LaunchedEffect(tenantId) { vm.load(tenantId) }
	LaunchedEffect(Unit) {
		vm.events.collect { event ->
			when (event) {
				is AdminUiEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
				is AdminUiEvent.ShowSuccess -> snackbarHostState.showSnackbar(event.message)
				is AdminUiEvent.NavigateToTemplateDetail -> Unit
				AdminUiEvent.CloseDialog -> Unit
			}
		}
	}

	Scaffold(
		snackbarHost = { SnackbarHost(snackbarHostState) }
	) { padding ->
		Column(
			Modifier
				.fillMaxSize()
				.padding(padding)
				.padding(16.dp)
		) {
			Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
				TextButton(onClick = onBack) { Text("← Dashboard") }
				Button(
					onClick = onCreate,
					enabled = !st.loading
				) { Text("Crear template") }
			}

			Spacer(Modifier.height(8.dp))
			Text("Templates · $tenantId", style = MaterialTheme.typography.titleLarge)

			st.error?.let { errorMessage ->
				Spacer(Modifier.height(8.dp))
				ErrorWithRetry(
					message = errorMessage,
					onRetry = { vm.load(tenantId) }
				)
			}

			Spacer(Modifier.height(12.dp))

			Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
				FilterChip(
					selected = st.statusFilter == null,
					onClick = { vm.setFilter(null, tenantId) },
					label = { Text("Todos") }
				)
				FilterChip(
					selected = st.statusFilter == TemplateStatus.DRAFT,
					onClick = { vm.setFilter(TemplateStatus.DRAFT, tenantId) },
					label = { Text("Draft") }
				)
				FilterChip(
					selected = st.statusFilter == TemplateStatus.PUBLISHED,
					onClick = { vm.setFilter(TemplateStatus.PUBLISHED, tenantId) },
					label = { Text("Published") }
				)
			}

			Spacer(Modifier.height(12.dp))
			if (st.loading) LinearProgressIndicator(Modifier.fillMaxWidth())

			Spacer(Modifier.height(12.dp))

			if (!st.loading && st.templates.isEmpty()) {
				EmptyState(
					message = "No hay templates para este tenant.",
					modifier = Modifier.weight(1f)
				)
			} else {
				LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
					items(st.templates, key = { it.id }) { t ->
						TemplateCard(
							t = t,
							enabled = !st.loading,
							onOpen = { onOpenTemplate(t.id) },
							onToggle = { vm.togglePublish(tenantId, t) }
						)
					}
				}
			}
		}
	}
}

@Composable
private fun TemplateCard(
	t: SurveyTemplateDto,
	enabled: Boolean,
	onOpen: () -> Unit,
	onToggle: () -> Unit
) {
	Card(Modifier.fillMaxWidth()) {
		Column(Modifier.padding(12.dp)) {
			Text(t.name, style = MaterialTheme.typography.titleMedium)
			Spacer(Modifier.height(4.dp))
			Text("Status: ${t.status} · Scope: ${t.scope ?: "-"}")

			t.location?.let { loc ->
				Spacer(Modifier.height(2.dp))
				Text("Location: ${loc.name}")
			}

			Spacer(Modifier.height(10.dp))
			val btnLabel = if (t.status == TemplateStatus.PUBLISHED) "Unpublish" else "Publish"
			Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
				TextButton(
					onClick = onOpen,
					enabled = enabled
				) { Text("Abrir") }
				Button(
					onClick = onToggle,
					enabled = enabled
				) { Text(btnLabel) }
			}
		}
	}
}
