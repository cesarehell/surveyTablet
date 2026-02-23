package com.mr.restaurant.survey.admin

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AdminApp : Application() {
	override fun onCreate() {
		super.onCreate()
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val nm = getSystemService(NotificationManager::class.java)
			nm.createNotificationChannel(
				NotificationChannel(
					"admin_alerts",
					"Alertas Admin",
					NotificationManager.IMPORTANCE_HIGH
				)
			)
		}
	}
}
