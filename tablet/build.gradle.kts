plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.android")
	id("org.jetbrains.kotlin.plugin.serialization")
	id("org.jetbrains.kotlin.plugin.compose")
	id("com.google.gms.google-services")
}
android {
	namespace = "com.mr.restaurant.survey"
	compileSdk = 35

	defaultConfig {
		applicationId = "com.mr.restaurant.survey"
		minSdk = 26
		targetSdk = 34
		versionCode = 1
		versionName = "1.0"

		buildConfigField("String", "DEFAULT_BASE_URL", "\"http://192.168.100.69:3040/\"")
		buildConfigField("String", "DEFAULT_TENANT", "\"mr-pez-1771377223\"")
		vectorDrawables { useSupportLibrary = true }
	}

	buildTypes {
		debug { isMinifyEnabled = false }
		release {
			isMinifyEnabled = true
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
		}
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}
	kotlinOptions { jvmTarget = "17" }

	buildFeatures {
		compose = true
		buildConfig = true
	}

	packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
	implementation(project(":core"))

	implementation(platform("androidx.compose:compose-bom:2024.09.02"))
	implementation("androidx.activity:activity-compose:1.9.2")
	implementation("androidx.compose.ui:ui")
	implementation("androidx.compose.ui:ui-tooling-preview")
	debugImplementation("androidx.compose.ui:ui-tooling")
	implementation("androidx.compose.material3:material3:1.3.0")

	implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
	implementation("com.google.firebase:firebase-database")
	implementation("com.google.firebase:firebase-messaging-ktx")

	implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
	implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

	implementation("com.google.android.material:material:1.12.0")

	implementation("com.squareup.retrofit2:retrofit:2.11.0")
	implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

	implementation("com.squareup.okhttp3:okhttp:4.12.0")
	implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
	implementation("androidx.datastore:datastore-preferences:1.1.1")
	implementation("com.journeyapps:zxing-android-embedded:4.3.0")
}
