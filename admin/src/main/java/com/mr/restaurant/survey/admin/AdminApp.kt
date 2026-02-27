package com.mr.restaurant.survey.admin

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.ApplicationInfo
import android.os.Build
import com.mr.restaurant.survey.core.net.ApiConfig
import dagger.hilt.android.HiltAndroidApp
import okhttp3.logging.HttpLoggingInterceptor

@HiltAndroidApp
class AdminApp : Application() {
	override fun onCreate() {
		super.onCreate()
		val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
		ApiConfig.httpLogLevel = if (isDebuggable) {
			HttpLoggingInterceptor.Level.BASIC
		} else {
			HttpLoggingInterceptor.Level.NONE
		}
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
