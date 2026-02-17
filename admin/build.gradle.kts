plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.android")
	id("org.jetbrains.kotlin.plugin.serialization")
	id("org.jetbrains.kotlin.plugin.compose")
}

android {
	namespace = "com.mr.restaurant.survey.admin"
	compileSdk = 34

	defaultConfig {
		applicationId = "com.mr.restaurant.survey.admin"
		minSdk = 26
		targetSdk = 34
		versionCode = 1
		versionName = "0.1"
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}
	kotlinOptions {
		jvmTarget = "17"
	}

	buildFeatures {
		compose = true
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

	implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
	implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
