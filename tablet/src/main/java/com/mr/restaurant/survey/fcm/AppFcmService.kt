package com.mr.restaurant.survey.fcm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mr.restaurant.survey.BuildConfig
import com.mr.restaurant.survey.R
import com.mr.restaurant.survey.data.SurveyRepository
import com.mr.restaurant.survey.net.Network
import com.mr.restaurant.survey.ui.TabletProvisioningPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppFcmService : FirebaseMessagingService() {

	override fun onCreate() {
		super.onCreate()
		createNotificationChannel()
	}

	override fun onNewToken(token: String) {
		Log.i("FCM", "Nuevo token: $token")
		registerOnly(applicationContext, token)
	}

	override fun onMessageReceived(message: RemoteMessage) {
		val title = message.notification?.title ?: "Nueva alerta"
		val body = message.notification?.body ?: message.data["reason"] ?: "Incidencia detectada"
		showNotification(title, body)
	}

	private fun createNotificationChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val channelId = "alerts_channel"
			val name = "Alerts"
			val description = "Alert notifications"
			val importance = NotificationManager.IMPORTANCE_HIGH

			val ch = NotificationChannel(channelId, name, importance).apply {
				this.description = description
			}

			val manager = getSystemService(NotificationManager::class.java)
			manager.createNotificationChannel(ch)
		}
	}

	private fun showNotification(title: String, body: String) {
		val notif =
			NotificationCompat.Builder(this, "alerts_channel").setSmallIcon(R.mipmap.ic_launcher)
				.setContentTitle(title).setContentText(body)
				.setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build()

		val nm = NotificationManagerCompat.from(this)
		if (Build.VERSION.SDK_INT < 33 || ActivityCompat.checkSelfPermission(
				this,
				Manifest.permission.POST_NOTIFICATIONS
			) == PackageManager.PERMISSION_GRANTED
		) {
			nm.notify(System.currentTimeMillis().toInt(), notif)
		} else {
			Log.w("FCM", "Sin permiso POST_NOTIFICATIONS")
		}
	}

	companion object {
		private val scope = CoroutineScope(Dispatchers.IO)
		private fun topicForTenant(tenantId: String) = "tenant-$tenantId"

		fun syncRegistration(context: Context) {
			FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
				Log.i("FCM", "FCM TOKEN: $token")
				registerOnly(context.applicationContext, token)
			}.addOnFailureListener { Log.e("FCM", "No se pudo obtener token", it) }
		}

		fun fetchToken(context: Context) {
			syncRegistration(context)
		}

		private fun registerOnly(context: Context, token: String) {
			val cfg = TabletProvisioningPrefs(context).load()
			if (cfg == null) {
				Log.w("FCM", "Tablet sin provisioning. Se omite registro de token hasta configurar tenant/location")
				FirebaseMessaging.getInstance().unsubscribeFromTopic("tenant-mr-restaurant")
					.addOnSuccessListener { Log.i("FCM", "Desuscrito de topic legacy tenant-mr-restaurant") }
					.addOnFailureListener { Log.w("FCM", "No se pudo desuscribir de topic legacy tenant-mr-restaurant", it) }
				return
			}
			val topic = topicForTenant(cfg.tenantId)
			FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
				.addOnSuccessListener {
					Log.i("FCM", "Desuscrito de topic $topic (tablet no recibe alertas de admin)")
				}
				.addOnFailureListener {
					Log.w("FCM", "No se pudo desuscribir de topic $topic", it)
				}
			FirebaseMessaging.getInstance().unsubscribeFromTopic("tenant-mr-restaurant")
				.addOnSuccessListener { Log.i("FCM", "Desuscrito de topic legacy tenant-mr-restaurant") }
				.addOnFailureListener { Log.w("FCM", "No se pudo desuscribir de topic legacy tenant-mr-restaurant", it) }
			register(cfg.tenantId, cfg.locationId, token)
		}

		private fun register(tenantId: String, locationId: String, token: String) {
			val api = Network.createApi(BuildConfig.DEFAULT_BASE_URL)
			val repo = SurveyRepository(api)
			scope.launch {
				runCatching {
					repo.registerDevice(
						tenantId = tenantId,
						token = token,
						owner = "Tablet",
						role = "MANAGER",
						locationId = locationId
					)
				}.onSuccess {
					Log.i("FCM", "Token registrado en backend tenant=$tenantId locationId=$locationId")
				}.onFailure {
					Log.e("FCM", "Error registrando token", it)
				}
			}
		}
	}
}
