package com.mr.restaurant.survey.admin.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mr.restaurant.survey.admin.R
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class AdminFcmService : FirebaseMessagingService() {

	@Inject
	lateinit var registrar: AdminPushRegistrar
	@Inject
	lateinit var prefs: AdminPushPrefs

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
		val title = buildTitle(message)
		val body = buildBody(message)
		showNotification(title, body, alertId)
	}

	private fun ensureChannel() {
		val manager = getSystemService(NotificationManager::class.java)
		manager.createNotificationChannel(
			NotificationChannel(
				CHANNEL_ID,
				"Alertas Admin",
				NotificationManager.IMPORTANCE_HIGH
			)
		)
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
			.setSmallIcon(R.drawable.ic_admin_notification)
			.setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_admin_launcher))
			.setContentTitle(title)
			.setContentText(body)
			.setStyle(NotificationCompat.BigTextStyle().bigText(body))
			.setPriority(NotificationCompat.PRIORITY_HIGH)
			.setAutoCancel(true)
			.build()
		val notificationId = alertId?.hashCode() ?: System.currentTimeMillis().toInt()
		nm.notify(notificationId, notification)
	}

	private companion object {
		const val CHANNEL_ID = "admin_alerts"
	}

	private fun buildTitle(message: RemoteMessage): String {
		message.notification?.title?.takeIf { it.isNotBlank() }?.let { return it }
		val severity = message.data["severity"]?.trim().orEmpty()
		if (severity.isBlank()) return "Nueva alerta"
		val formattedSeverity = severity.lowercase(Locale.getDefault()).replaceFirstChar {
			if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
		}
		return "Alerta $formattedSeverity"
	}

	private fun buildBody(message: RemoteMessage): String {
		val reasonRaw = message.data["reason"]?.trim().orEmpty()
		val notificationBody = message.notification?.body?.trim().orEmpty()
		val reason = humanizeReason(reasonRaw.ifBlank { notificationBody })
		val location = message.data["locationName"]?.trim().orEmpty()
		val tableNo = message.data["tableNo"]?.trim().orEmpty()
		val waiter = message.data["waiterName"]?.trim().orEmpty()

		val parts = buildList {
			if (location.isNotBlank()) add("Sucursal: $location")
			if (tableNo.isNotBlank()) add("Mesa: $tableNo")
			if (waiter.isNotBlank()) add("Mesero: $waiter")
			if (reason.isNotBlank()) add(reason)
		}
		return parts.joinToString(" · ").ifBlank { "Se detectó una alerta operativa" }
	}

	private fun humanizeReason(raw: String): String {
		if (raw.isBlank()) return raw
		val normalized = raw.trim()
		val regex = Regex("""(?i)score\s+([0-9]+(?:\.[0-9]+)?)\s+triggered\s+rule\s+([A-Z_]+)(?:\s*\(([^)]*)\))?""")
		val match = regex.find(normalized)
		if (match != null) {
			val score = match.groupValues.getOrNull(1).orEmpty()
			val rule = match.groupValues.getOrNull(2).orEmpty().uppercase(Locale.getDefault())
			val threshold = match.groupValues.getOrNull(3).orEmpty()
			val ruleLabel = when (rule) {
				"NEGATIVE" -> "Calificación negativa"
				"LT" -> "Menor que umbral"
				"LE" -> "Menor o igual al umbral"
				"EQ" -> "Igual al umbral"
				"GE" -> "Mayor o igual al umbral"
				else -> "Regla $rule"
			}
			val detail = threshold.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
			return "Se disparó $ruleLabel · score $score$detail"
		}
		return normalized
	}
}
