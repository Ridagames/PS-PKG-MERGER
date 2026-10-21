plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.venice.pkgmerger"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.venice.pkgmerger"
        minSdk = 26
        targetSdk = 34
        versionCode = 14
        versionName = "2.3"
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
