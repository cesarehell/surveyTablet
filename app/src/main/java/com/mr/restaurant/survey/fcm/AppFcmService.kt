package com.mr.restaurant.survey.fcm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppFcmService : FirebaseMessagingService() {

	private val scope = CoroutineScope(Dispatchers.IO)

	override fun onCreate() {
		super.onCreate()
		createNotificationChannel()
	}

	override fun onNewToken(token: String) {
		Log.i("FCM", "Nuevo token: $token")
		FirebaseMessaging.getInstance().subscribeToTopic("tenant-mr-restaurant")
			.addOnSuccessListener { Log.i("FCM", "Suscrito a topic tenant-mr-restaurant") }
			.addOnFailureListener { Log.w("FCM", "No se pudo suscribir a topic", it) }
		register(token)
	}

	private fun register(token: String) {
		val api = Network.createApi(BuildConfig.DEFAULT_BASE_URL)
		val repo = SurveyRepository(api)
		scope.launch {
			runCatching {
				repo.registerDevice(BuildConfig.DEFAULT_TENANT, token, owner = "Tablet")
			}.onSuccess {
				Log.i("FCM", "Token registrado en backend")
			}.onFailure {
				Log.e("FCM", "Error registrando token", it)
			}
		}
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
		fun fetchToken() {
			FirebaseMessaging.getInstance().token.addOnSuccessListener {
				Log.i(
					"FCM",
					"FCM TOKEN: $it"
				)
			}.addOnFailureListener { Log.e("FCM", "No se pudo obtener token", it) }
		}
	}
}