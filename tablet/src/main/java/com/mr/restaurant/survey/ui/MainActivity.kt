package com.mr.restaurant.survey.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mr.restaurant.survey.BuildConfig
import com.mr.restaurant.survey.data.SurveyRepository
import com.mr.restaurant.survey.fcm.AppFcmService
import com.mr.restaurant.survey.net.Network

class MainActivity : ComponentActivity() {

	private val vm: AppViewModel by viewModels { appViewModelFactory() }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		ensureNotifChannel()
		requestNotifPermissionIfNeeded()

		AppFcmService.fetchToken()

		setContent { SurveyKiosk(vm) }
	}

	private fun appViewModelFactory(): ViewModelProvider.Factory {
		val api = Network.createApi(BuildConfig.DEFAULT_BASE_URL)
		val repo = SurveyRepository(api)

		val tenant = BuildConfig.DEFAULT_TENANT
		val templateId: String? = null
		val locationId: String? = null

		val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
		Log.d("DEVICE", "DEVICE ID = $deviceId")

		return object : ViewModelProvider.Factory {
			override fun <T : ViewModel> create(modelClass: Class<T>): T {
				@Suppress("UNCHECKED_CAST")
				return AppViewModel(
					repo = repo,
					tenant = tenant,
					templateId = templateId,
					locationId = locationId,
					appContext = applicationContext,
				) as T
			}
		}
	}

	private fun ensureNotifChannel() {
		if (Build.VERSION.SDK_INT >= 26) {
			val nm = getSystemService(NotificationManager::class.java)
			nm.createNotificationChannel(
				NotificationChannel(
					"alerts_channel",
					"Alertas",
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
				requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
			}
		}
	}
}
