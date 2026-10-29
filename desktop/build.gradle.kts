plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose") version "1.5.11"
}

kotlin {
    jvm("desktop") {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }
    
    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                // Tu y ajoutes la référence vers ton code partagé ou ton module d'UI
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.venice.pkgmerger.MainKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe
            )
            packageName = "PS5 PKG Merger"
            packageVersion = "2.1.0"
        }
    }
}
