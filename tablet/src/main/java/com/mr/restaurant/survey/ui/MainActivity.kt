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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mr.restaurant.survey.BuildConfig
import com.mr.restaurant.survey.data.SurveyRepository
import com.mr.restaurant.survey.fcm.AppFcmService
import com.mr.restaurant.survey.net.Network

class MainActivity : ComponentActivity() {
	private lateinit var provisioningPrefs: TabletProvisioningPrefs

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		provisioningPrefs = TabletProvisioningPrefs(applicationContext)

		ensureNotifChannel()
		requestNotifPermissionIfNeeded()
		val initialConfig = provisioningPrefs.load()
		if (initialConfig != null) {
			AppFcmService.fetchToken(applicationContext)
		}

		setContent {
			var config = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(initialConfig) }
			val currentConfig = config.value
			if (currentConfig == null) {
				TabletProvisioningScreen(
					defaultTenantId = BuildConfig.DEFAULT_TENANT,
					baseUrl = BuildConfig.DEFAULT_BASE_URL
				) { saved ->
					provisioningPrefs.save(saved)
					config.value = saved
					AppFcmService.fetchToken(applicationContext)
				}
			} else {
				val vm: AppViewModel = viewModel(
					key = "tablet-${currentConfig.tenantId}-${currentConfig.locationId}",
					factory = appViewModelFactory(currentConfig)
				)
				SurveyKiosk(vm)
			}
		}
	}

	private fun appViewModelFactory(config: TabletProvisioningConfig): ViewModelProvider.Factory {
		val api = Network.createApi(BuildConfig.DEFAULT_BASE_URL)
		val repo = SurveyRepository(api)

		val tenant = config.tenantId
		val templateId: String? = null
		val locationId: String? = config.locationId

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
