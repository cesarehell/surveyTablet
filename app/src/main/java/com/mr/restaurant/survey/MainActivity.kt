package com.mr.restaurant.survey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mr.restaurant.survey.data.SurveyRepository
import com.mr.restaurant.survey.net.Network
import com.mr.restaurant.survey.BuildConfig
import com.mr.restaurant.survey.ui.AppViewModel
import com.mr.restaurant.survey.ui.SurveyKiosk

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels {
        val api = Network.createApi(BuildConfig.DEFAULT_BASE_URL)
        val repo = SurveyRepository(api)
        val tenant = BuildConfig.DEFAULT_TENANT

        val templateId: String? = null
        val locationId: String? = null

        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AppViewModel(
                    repo = repo,
                    tenant = tenant,
                    templateId = templateId,
                    locationId = locationId
                ) as T
            }
        }
    }

    private fun ensureNotifChannel() {
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(android.app.NotificationManager::class.java)
            nm.createNotificationChannel(
                android.app.NotificationChannel(
                    "alerts_channel", "Alertas",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            val granted = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        AppFcmService.fetchToken()

        setContent { SurveyKiosk(vm) }
    }
}
