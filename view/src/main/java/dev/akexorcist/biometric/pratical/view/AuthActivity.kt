package dev.akexorcist.biometric.pratical.view

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dev.akexorcist.biometric.pratical.view.databinding.ActivityAuthBinding
import java.util.UUID

class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding

    private var currentToken: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonRandomizeToken.setOnClickListener {
            randomizeToken()
        }

        binding.buttonEnableBiometric.setOnClickListener {
            binding.buttonAuthenticate.isEnabled = true
            binding.buttonDisableBiometric.isEnabled = true
            binding.buttonEnableBiometric.isEnabled = false
            Toast.makeText(this, "Biometric enabled (simulated)", Toast.LENGTH_SHORT).show()
        }

        binding.buttonDisableBiometric.setOnClickListener {
            binding.buttonAuthenticate.isEnabled = false
            binding.buttonDisableBiometric.isEnabled = false
            binding.buttonEnableBiometric.isEnabled = true
            Toast.makeText(this, "Biometric disabled (simulated)", Toast.LENGTH_SHORT).show()
        }

        binding.buttonAuthenticate.setOnClickListener {
            startActivity(HomeActivity.newIntent(this, currentToken))
        }
        randomizeToken()
    }

    private fun randomizeToken() {
        currentToken = UUID.randomUUID().toString()
        binding.textViewToken.text = currentToken
    }
}
