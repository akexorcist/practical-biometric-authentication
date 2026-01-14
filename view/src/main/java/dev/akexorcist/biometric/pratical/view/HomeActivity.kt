package dev.akexorcist.biometric.pratical.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dev.akexorcist.biometric.pratical.view.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_TOKEN = "extra_token"

        fun newIntent(context: Context, decryptedToken: String): Intent {
            return Intent(context, HomeActivity::class.java).apply {
                putExtra(EXTRA_TOKEN, decryptedToken)
            }
        }
    }

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val token = intent.getStringExtra(EXTRA_TOKEN)
        binding.textViewDecryptedToken.text = token

        binding.buttonBack.setOnClickListener {
            finish()
        }
    }
}
