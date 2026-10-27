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

// Tâche ultra-robuste : nettoie automatiquement tout caractère non valide au début des ressources
tasks.register("sanitizeResourceNames") {
    doLast {
        val resDir = file("src/main/res")
        if (resDir.exists()) {
            resDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val name = file.name
                    // Si le nom ne commence pas par une lettre minuscule de a à z
                    if (!name.matches(Regex("^[a-z].*"))) {
                        // Supprime tous les caractères spéciaux / chiffres / symboles au début et met en minuscules
                        val cleanName = name.replace(Regex("^[^a-zA-Z]+"), "").lowercase()
                        if (cleanName.isNotEmpty() && cleanName != name) {
                            val newFile = File(file.parent, cleanName)
                            if (file.renameTo(newFile)) {
                                println("✅ Nettoyage automatique : '$name' a été renommé proprement en '$cleanName'")
                            }
                        }
                    }
                }
            }
        }
    }
}

// S'exécute avant la construction pour intercepter et corriger les fichiers à la volée
tasks.named("preBuild") {
    dependsOn("sanitizeResourceNames")
}
