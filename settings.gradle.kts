pluginManagement {
	repositories {
		google()
		mavenCentral()
		gradlePluginPortal()
	}
	plugins {
		id("com.android.application") version "8.10.0" apply false
		id("com.android.library") version "8.10.0" apply false
		id("org.jetbrains.kotlin.android") version "2.0.20" apply false
		id("org.jetbrains.kotlin.plugin.serialization") version "2.0.20" apply false
		id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
		id("com.google.gms.google-services") version "4.4.2" apply false
	}
}

dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
	repositories {
		google()
		mavenCentral()
	}
}

rootProject.name = "SurveyTabletApp"
include(":core")
include(":admin")
include(":tablet")
