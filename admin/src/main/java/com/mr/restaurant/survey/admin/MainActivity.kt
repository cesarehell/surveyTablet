package com.mr.restaurant.survey.admin

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mr.restaurant.survey.admin.push.AdminPushRegistrar
import com.mr.restaurant.survey.admin.ui.theme.AdminTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
	@Inject lateinit var pushRegistrar: AdminPushRegistrar

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		ensureNotifChannel()
		requestNotifPermissionIfNeeded()
		pushRegistrar.tryRegisterCurrentTenant()
		setContent {
			AdminTheme(darkTheme = false) {
				AdminNavHost()
			}
		}
	}

	private fun ensureNotifChannel() {
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

	private fun requestNotifPermissionIfNeeded() {
		if (Build.VERSION.SDK_INT >= 33) {
			val granted = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
				PackageManager.PERMISSION_GRANTED
			if (!granted) {
				requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1002)
			}
		}
	}
}
