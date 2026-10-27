package com.example.ps5pkgmerger

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Références des boutons du menu
        val btnPs3 = findViewById<Button>(R.id.btnPs3)
        val btnPs4 = findViewById<Button>(R.id.btnPs4)
        val btnPs5 = findViewById<Button>(R.id.btnPs5)
        val btnSplitter = findViewById<Button>(R.id.btnSplitter)

        btnPs3.setOnClickListener {
            Toast.makeText(this, "Mode PS3 sélectionné", Toast.LENGTH_SHORT).show()
            // TODO: Lancer l'activité ou la logique PS3
        }

        btnPs4.setOnClickListener {
            Toast.makeText(this, "Mode PS4 sélectionné", Toast.LENGTH_SHORT).show()
            // TODO: Lancer l'activité ou la logique PS4
        }

        btnPs5.setOnClickListener {
            Toast.makeText(this, "Mode PS5 sélectionné", Toast.LENGTH_SHORT).show()
            // TODO: Lancer l'activité ou la logique PS5
        }

        btnSplitter.setOnClickListener {
            Toast.makeText(this, "Outil Splitter sélectionné", Toast.LENGTH_SHORT).show()
            
            // Exemple d'utilisation de la fonction SHA-256 :
            // Remplace par le chemin réel de ton fichier fusionné ou découpé
            /*
            val targetFile = File(getExternalFilesDir(null), "mon_fichier.pkg")
            if (targetFile.exists()) {
                val hash = calculateSha256(targetFile.absolutePath)
                Toast.makeText(this, "SHA-256 : $hash", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Fichier cible introuvable pour la vérification", Toast.LENGTH_SHORT).show()
            }
            */
        }
    }

    /**
     * Calcule le hachage SHA-256 d'un fichier donné par son chemin d'accès.
     */
    private fun calculateSha256(filePath: String): String? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null

            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            // Convertit les bytes du hash en chaîne hexadécimale
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
