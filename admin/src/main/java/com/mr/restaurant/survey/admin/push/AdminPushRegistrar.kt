package com.mr.restaurant.survey.admin.push

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.mr.restaurant.survey.core.device.api.DeviceApi
import com.mr.restaurant.survey.core.device.dto.RegisterDeviceRequest
import com.mr.restaurant.survey.core.net.ApiResult
import com.mr.restaurant.survey.core.net.safeCall
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminPushRegistrar @Inject constructor(
	private val deviceApi: DeviceApi,
	private val prefs: AdminPushPrefs,
	@ApplicationContext private val context: Context,
) {
	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

	fun bindTenant(tenantId: String) {
		prefs.saveTenantId(tenantId)
		registerCurrentTokenForTenant(tenantId)
	}

	fun onNewToken(token: String) {
		val tenantId = prefs.getTenantId() ?: run {
			Log.i(TAG, "Token FCM recibido, pero no hay tenant seleccionado aún")
			return
		}
		registerToken(tenantId, token)
	}

	fun tryRegisterCurrentTenant() {
		val tenantId = prefs.getTenantId() ?: return
		registerCurrentTokenForTenant(tenantId)
	}

	private fun registerCurrentTokenForTenant(tenantId: String) {
		val hasFirebase = runCatching { FirebaseApp.getApps(context).isNotEmpty() }.getOrDefault(false)
		if (!hasFirebase) {
			Log.w(TAG, "Firebase no inicializado en admin. Falta google-services.json compatible con el package del app")
			return
		}
		val topic = topicForTenant(tenantId)
		FirebaseMessaging.getInstance().subscribeToTopic(topic)
			.addOnSuccessListener { Log.i(TAG, "Suscrito localmente a topic $topic") }
			.addOnFailureListener { err -> Log.w(TAG, "No se pudo suscribir localmente a topic $topic", err) }

		FirebaseMessaging.getInstance().token
			.addOnSuccessListener { token ->
				registerToken(tenantId, token)
			}
			.addOnFailureListener { err ->
				Log.e(TAG, "No se pudo obtener token FCM para tenant=$tenantId", err)
			}
	}

	private fun registerToken(tenantId: String, token: String) {
		scope.launch {
			val req = RegisterDeviceRequest(
				tenantId = tenantId,
				token = token,
				owner = "AdminApp",
				platform = "ANDROID",
				role = "OWNER",
				deviceType = "ADMIN"
			)
			when (val res = safeCall { deviceApi.register(req) }) {
				is ApiResult.Ok -> {
					Log.i(TAG, "FCM admin registrado tenant=$tenantId topic=${res.value.topic} subscribed=${res.value.pushSubscribed}")
				}
				is ApiResult.Err -> {
					Log.e(TAG, "Error registrando token admin tenant=$tenantId: ${res.message}")
				}
			}
		}
	}

	private companion object {
		const val TAG = "AdminPushRegistrar"
		fun topicForTenant(tenantId: String) = "tenant-$tenantId"
	}
}
