import java.io.File

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
        versionCode = 23
        versionName = "3.2"
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

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.documentfile:documentfile:1.0.1")
}

// Tâche automatique pour nettoyer les noms de fichiers (supprime les ? au début)
tasks.register("sanitizeResourceNames") {
    doLast {
        val resDirs = fileTree("src/main/res")
        resDirs.forEach { file ->
            if (file.isFile && file.name.startsWith("?")) {
                val cleanName = file.name.replace("?", "")
                val newFile = File(file.parent, cleanName)
                if (file.renameTo(newFile)) {
                    println("✅ Nettoyage automatique : ${file.name} renommé en $cleanName")
                }
            }
        }
    }
}

// Force l'exécution de ce nettoyage avant chaque compilation
tasks.named("preBuild") {
    dependsOn("sanitizeResourceNames")
}
