package com.mr.restaurant.survey.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.mr.restaurant.survey.BuildConfig
import com.mr.restaurant.survey.data.SurveyRepository
import com.mr.restaurant.survey.fcm.AppFcmService
import com.mr.restaurant.survey.net.Network

class MainActivity : ComponentActivity() {
	private lateinit var provisioningPrefs: TabletProvisioningPrefs

	@SuppressLint("HardwareIds")
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		provisioningPrefs = TabletProvisioningPrefs(applicationContext)
		val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
		Log.d("DEVICE", "DEVICE ID = $deviceId")

		ensureNotifChannel()
		requestNotifPermissionIfNeeded()
		val initialConfig = provisioningPrefs.load()
		val provisioningRepo = SurveyRepository(Network.createApi(BuildConfig.DEFAULT_BASE_URL))
		if (initialConfig != null) {
			AppFcmService.fetchToken(applicationContext)
		}

		setContent {
			val config = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(initialConfig) }
			val provisioningPrefill =
				androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(initialConfig) }
			val scannedPairingCode =
				androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
			val qrLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
				val content = result.contents?.trim()?.takeIf { it.isNotBlank() }
				scannedPairingCode.value = content
			}
			val currentConfig = config.value
			if (currentConfig == null) {
				TabletProvisioningScreen(
					initialConfig = provisioningPrefill.value,
					defaultTenantId = BuildConfig.DEFAULT_TENANT,
					baseUrl = BuildConfig.DEFAULT_BASE_URL,
					deviceId = deviceId,
					scannedPairingCode = scannedPairingCode.value,
					onRequestQrScan = {
						val options = ScanOptions().apply {
							setDesiredBarcodeFormats(ScanOptions.QR_CODE)
							setPrompt("Escanea el QR de pairing")
							setBeepEnabled(false)
							setOrientationLocked(false)
							setBarcodeImageEnabled(false)
						}
						qrLauncher.launch(options)
					},
					onConsumeScannedPairingCode = {
						scannedPairingCode.value = null
					},
					onLoadLocations = { tenantId -> provisioningRepo.listLocations(tenantId) },
					onPairTablet = { pairingCode, currentDeviceId ->
						provisioningRepo.pairTablet(pairingCode, currentDeviceId)
					}
				) { saved ->
					provisioningPrefs.save(saved)
					provisioningPrefill.value = saved
					config.value = saved
					AppFcmService.fetchToken(applicationContext)
				}
			} else {
				val vmFactory = remember(currentConfig) { appViewModelFactory(currentConfig) }
				val vm: AppViewModel = viewModel(
					key = "tablet-${currentConfig.tenantId}-${currentConfig.locationId}",
					factory = vmFactory
				)
				SurveyKiosk(
					vm = vm,
					onReconfigureTablet = {
						provisioningPrefill.value = currentConfig
						config.value = null
					},
					onCloseApp = { finish() }
				)
			}
		}
	}

	@SuppressLint("HardwareIds")
	private fun appViewModelFactory(config: TabletProvisioningConfig): ViewModelProvider.Factory {
		val api = Network.createApi(BuildConfig.DEFAULT_BASE_URL)
		val repo = SurveyRepository(api)
		val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
		val deviceOwner = "Tablet:$deviceId"

		val tenant = config.tenantId
		val templateId: String? = null
		val locationId: String? = config.locationId
		val table = config.tableNo?.takeIf { it.isNotBlank() }.orEmpty()
		val waiter = config.waiterName?.takeIf { it.isNotBlank() }.orEmpty()
		val waiters = config.waiters

		return object : ViewModelProvider.Factory {
			override fun <T : ViewModel> create(modelClass: Class<T>): T {
				@Suppress("UNCHECKED_CAST")
				return AppViewModel(
					repo = repo,
					tenant = tenant,
					templateId = templateId,
					locationId = locationId,
					deviceOwner = deviceOwner,
					appContext = applicationContext,
					table = table,
					waiter = waiter,
					knownWaiters = waiters,
				) as T
			}
		}
	}

	private fun ensureNotifChannel() {
		val nm = getSystemService(NotificationManager::class.java)
		nm.createNotificationChannel(
			NotificationChannel(
				"alerts_channel",
				"Alertas",
				NotificationManager.IMPORTANCE_HIGH
			)
		)
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
