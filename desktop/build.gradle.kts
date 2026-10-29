plugins {
    kotlin("jvm")
    id("org.jetbrains.compose") version "1.5.11"
}

dependencies {
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "com.venice.pkgmerger.desktop.MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe)
            packageName = "PS5 PKG Merger"
            packageVersion = "2.1.0"
        }
    }
}
