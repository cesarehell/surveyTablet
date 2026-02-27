package com.mr.restaurant.survey.admin.push

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.mr.restaurant.survey.admin.ui.SimpleTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class AdminPushDebugUiState(
	val loading: Boolean = false,
	val tenantId: String = "",
	val boundTenantId: String? = null,
	val notificationsEnabled: Boolean = false,
	val postNotificationsGranted: Boolean = true,
	val firebaseInitialized: Boolean = false,
	val fcmToken: String? = null,
	val lastLocalTopic: String? = null,
	val lastLocalTopicError: String? = null,
	val lastBackendTopic: String? = null,
	val lastBackendPushSubscribed: Boolean? = null,
	val lastBackendError: String? = null,
	val lastUpdatedAtMillis: Long? = null,
	val refreshMessage: String? = null,
)

@HiltViewModel
class AdminPushDebugViewModel @Inject constructor(
	private val registrar: AdminPushRegistrar,
	private val prefs: AdminPushPrefs,
	@ApplicationContext private val context: Context,
) : ViewModel() {
	private val _state = MutableStateFlow(AdminPushDebugUiState())
	val state = _state.asStateFlow()

	fun load(tenantId: String) {
		_state.update { it.copy(tenantId = tenantId) }
		refreshSnapshot()
		fetchToken()
	}

	fun retryRegister() {
		val tenantId = _state.value.tenantId
		if (tenantId.isBlank()) return
		_state.update { it.copy(loading = true, refreshMessage = "Reintentando registro…") }
		registrar.bindTenant(tenantId)
		viewModelScope.launch {
			kotlinx.coroutines.delay(1200)
			refreshSnapshot()
			fetchToken()
			_state.update { it.copy(loading = false, refreshMessage = "Registro reintentado") }
		}
	}

	fun clearDebug() {
		prefs.clearDebug()
		refreshSnapshot()
	}

	private fun refreshSnapshot() {
		val postGranted = if (Build.VERSION.SDK_INT >= 33) {
			ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
					android.content.pm.PackageManager.PERMISSION_GRANTED
		} else true

		_state.update {
			it.copy(
				boundTenantId = registrar.currentTenantId(),
				notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled(),
				postNotificationsGranted = postGranted,
				firebaseInitialized = runCatching { FirebaseApp.getApps(context).isNotEmpty() }.getOrDefault(false),
				lastLocalTopic = prefs.getLastLocalTopic(),
				lastLocalTopicError = prefs.getLastLocalTopicError(),
				lastBackendTopic = prefs.getLastTopic(),
				lastBackendPushSubscribed = prefs.getLastPushSubscribed(),
				lastBackendError = prefs.getLastError(),
				lastUpdatedAtMillis = prefs.getLastUpdatedAtMillis(),
			)
		}
	}

	private fun fetchToken() {
		viewModelScope.launch {
			val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
			if (token != null) prefs.saveLastToken(token)
			_state.update { it.copy(fcmToken = token ?: prefs.getLastToken()) }
		}
	}
}

@Composable
fun AdminPushDebugRoute(
	tenantId: String,
	onBack: () -> Unit,
	vm: AdminPushDebugViewModel = hiltViewModel(),
) {
	val st by vm.state.collectAsState()
	val snackbarHostState = remember { SnackbarHostState() }

	LaunchedEffect(tenantId) { vm.load(tenantId) }
	LaunchedEffect(st.refreshMessage) {
		st.refreshMessage?.let { snackbarHostState.showSnackbar(it) }
	}

	AdminPushDebugScreen(
		state = st,
		onBack = onBack,
		onRetryRegister = vm::retryRegister,
		onClearDebug = vm::clearDebug,
		snackbarHostState = snackbarHostState
	)
}

@Composable
private fun AdminPushDebugScreen(
	state: AdminPushDebugUiState,
	onBack: () -> Unit,
	onRetryRegister: () -> Unit,
	onClearDebug: () -> Unit,
	snackbarHostState: SnackbarHostState,
) {
	val rows = listOf(
		"Tenant (route)" to state.tenantId,
		"Tenant (prefs)" to (state.boundTenantId ?: "-"),
		"Notifications enabled" to state.notificationsEnabled.toString(),
		"POST_NOTIFICATIONS" to state.postNotificationsGranted.toString(),
		"Firebase initialized" to state.firebaseInitialized.toString(),
		"Token FCM" to (state.fcmToken ?: "-"),
		"Topic local" to (state.lastLocalTopic ?: "-"),
		"Error topic local" to (state.lastLocalTopicError ?: "-"),
		"Topic backend" to (state.lastBackendTopic ?: "-"),
		"Backend subscribed" to (state.lastBackendPushSubscribed?.toString() ?: "-"),
		"Error backend" to (state.lastBackendError ?: "-"),
		"Última actualización" to (state.lastUpdatedAtMillis?.toString() ?: "-"),
	)

	Scaffold(
		snackbarHost = { SnackbarHost(snackbarHostState) },
		topBar = {
			SimpleTopBar(
				title = "Push Debug",
				subtitle = state.tenantId,
				onBack = onBack
			)
		}
	) { padding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding)
				.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp)
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(8.dp)
			) {
				Button(onClick = onRetryRegister, modifier = Modifier.weight(1f)) {
					Text(if (state.loading) "Registrando…" else "Reintentar registro")
				}
				TextButton(onClick = onClearDebug, modifier = Modifier.weight(1f)) {
					Text("Limpiar debug")
				}
			}

			LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
				items(rows) { (label, value) ->
					Card(modifier = Modifier.fillMaxWidth()) {
						Column(
							modifier = Modifier.padding(12.dp),
							verticalArrangement = Arrangement.spacedBy(4.dp)
						) {
							Text(label, style = MaterialTheme.typography.labelMedium)
							Text(
								value,
								style = MaterialTheme.typography.bodyMedium,
								fontWeight = FontWeight.Medium,
								maxLines = 2,
								overflow = TextOverflow.Ellipsis
							)
						}
					}
				}
			}
		}
	}
}
