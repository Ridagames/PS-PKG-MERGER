package com.example.ps5pkgmerger

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity {

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
            // TODO: Lancer l'outil de découpage
        }
    }
}
