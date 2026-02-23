package com.mr.restaurant.survey.admin.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mr.restaurant.survey.admin.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AdminFcmService : FirebaseMessagingService() {

	@Inject lateinit var registrar: AdminPushRegistrar
	@Inject lateinit var prefs: AdminPushPrefs

	override fun onCreate() {
		super.onCreate()
		ensureChannel()
	}

	override fun onNewToken(token: String) {
		registrar.onNewToken(token)
	}

	override fun onMessageReceived(message: RemoteMessage) {
		val alertId = message.data["alertId"]?.takeIf { it.isNotBlank() }
		if (alertId != null && prefs.wasAlertSeenRecently(alertId)) {
			return
		}
		val title = message.notification?.title ?: "Nueva alerta"
		val body = message.notification?.body
			?: message.data["reason"]
			?: "Se detectó una calificación crítica"
		showNotification(title, body, alertId)
	}

	private fun ensureChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val manager = getSystemService(NotificationManager::class.java)
			manager.createNotificationChannel(
				NotificationChannel(
					CHANNEL_ID,
					"Alertas Admin",
					NotificationManager.IMPORTANCE_HIGH
				)
			)
		}
	}

	private fun showNotification(title: String, body: String, alertId: String?) {
		val nm = NotificationManagerCompat.from(this)
		if (Build.VERSION.SDK_INT >= 33 && ActivityCompat.checkSelfPermission(
				this,
				Manifest.permission.POST_NOTIFICATIONS
			) != PackageManager.PERMISSION_GRANTED
		) {
			return
		}

		val notification = NotificationCompat.Builder(this, CHANNEL_ID)
			.setSmallIcon(R.mipmap.ic_launcher)
			.setContentTitle(title)
			.setContentText(body)
			.setPriority(NotificationCompat.PRIORITY_HIGH)
			.setAutoCancel(true)
			.build()
		val notificationId = alertId?.hashCode() ?: System.currentTimeMillis().toInt()
		nm.notify(notificationId, notification)
	}

	private companion object {
		const val CHANNEL_ID = "admin_alerts"
	}
}
