plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.venice.pkgmerger"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.venice.pkgmerger"
        minSdk = 26
        targetSdk = 35
        versionCode = 9
        versionName = "1.8"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    buildTypes {
        release { isMinifyEnabled = false }
    }
}
