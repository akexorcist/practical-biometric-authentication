package dev.akexorcist.biometric.pratical.view

import android.content.Context
import android.os.Bundle
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.util.Base64
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import dev.akexorcist.biometric.pratical.shared.crypto.CryptographyManager
import dev.akexorcist.biometric.pratical.shared.data.AuthenticationDataRepository
import dev.akexorcist.biometric.pratical.shared.viewmodel.AuthUiState
import dev.akexorcist.biometric.pratical.shared.viewmodel.AuthViewModel
import dev.akexorcist.biometric.pratical.shared.viewmodel.ViewModelFactory
import dev.akexorcist.biometric.pratical.view.databinding.ActivityAuthBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.crypto.Cipher

class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding
    private val viewModel: AuthViewModel by viewModels { ViewModelFactory(AuthenticationDataRepository(this)) }
    private val cryptographyManager = CryptographyManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            viewModel.uiState.collectLatest { uiState ->
                updateUi(uiState)
            }
        }

        binding.buttonRandomizeToken.setOnClickListener {
            viewModel.onRandomizeTokenClick()
        }

        binding.buttonEnableBiometric.setOnClickListener {
            showBiometricPromptForEnable()
        }

        binding.buttonDisableBiometric.setOnClickListener {
            runCatching { cryptographyManager.deleteKey() }
            viewModel.onDisableBiometricClick()
        }

        binding.buttonAuthenticate.setOnClickListener {
            (viewModel.uiState.value as? AuthUiState.BiometricEnabled)?.let { uiState ->
                showBiometricPromptForAuthenticate(uiState)
            }
        }
    }

    private fun updateUi(uiState: AuthUiState) {
        when (uiState) {
            is AuthUiState.Initial -> {
                binding.textViewToken.text = uiState.token
                binding.buttonAuthenticate.isEnabled = false
                binding.buttonDisableBiometric.isEnabled = false
                binding.buttonEnableBiometric.isEnabled = true
                binding.buttonRandomizeToken.isEnabled = true
            }

            is AuthUiState.BiometricEnabled -> {
                binding.textViewToken.text = uiState.token
                binding.buttonAuthenticate.isEnabled = true
                binding.buttonDisableBiometric.isEnabled = true
                binding.buttonEnableBiometric.isEnabled = false
                binding.buttonRandomizeToken.isEnabled = false
            }
        }
    }

    private fun showBiometricPromptForEnable() {
        val canAuthenticate = checkBiometricAvailability()
        if (canAuthenticate) {
            val cipher = handleCipherResult(
                onInvalidKey = {
                    runCatching { cryptographyManager.deleteKey() }
                    viewModel.onDisableBiometricClick()
                },
            ) { cryptographyManager.getCipherForEncryption() }
                ?: return
            val biometricPrompt = createBiometricPrompt(
                onSuccess = { result ->
                    result.cryptoObject?.cipher?.let { cipher ->
                        val encryptedData = cryptographyManager.encrypt(viewModel.uiState.value.token.toByteArray(), cipher)
                        val encryptedToken = Base64.encodeToString(encryptedData.first, Base64.NO_WRAP)
                        val iv = Base64.encodeToString(encryptedData.second, Base64.NO_WRAP)
                        viewModel.onEnableBiometricSuccess(
                            encryptedToken = encryptedToken,
                            iv = iv,
                        )
                    }
                },
            )
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Enable Biometric Authentication")
                .setSubtitle("Confirm to enable biometric authentication")
                .setNegativeButtonText("Cancel")
                .setConfirmationRequired(false)
                .build()
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
    }

    private fun showBiometricPromptForAuthenticate(uiState: AuthUiState.BiometricEnabled) {
        val canAuthenticate = checkBiometricAvailability()
        if (canAuthenticate) {
            val iv = Base64.decode(uiState.encryptedData.iv, Base64.NO_WRAP)
            val cipher = handleCipherResult(
                onInvalidKey = {
                    runCatching { cryptographyManager.deleteKey() }
                    viewModel.onDisableBiometricClick()
                },
            ) { cryptographyManager.getCipherForDecryption(iv) }
                ?: return
            val biometricPrompt = createBiometricPrompt(
                onSuccess = { result ->
                    result.cryptoObject?.cipher?.let { cipher ->
                        val encryptedToken = Base64.decode(uiState.encryptedData.encryptedToken, Base64.NO_WRAP)
                        val decryptedToken = cryptographyManager.decrypt(encryptedToken, cipher)
                        startActivity(HomeActivity.newIntent(this, String(decryptedToken)))
                    }
                },
            )
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Authentication")
                .setSubtitle("Authenticate to log in")
                .setNegativeButtonText("Cancel")
                .setConfirmationRequired(false)
                .build()
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
    }

    private fun createBiometricPrompt(
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
    ): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(this)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess(result)
            }

            override fun onAuthenticationFailed() {
                Toast.makeText(this@AuthActivity, "Authentication failed", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Toast.makeText(this@AuthActivity, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
            }
        }
        return BiometricPrompt(this, executor, callback)
    }

    private fun handleCipherResult(
        onInvalidKey: () -> Unit,
        block: () -> Cipher,
    ): Cipher? {
        return try {
            block()
        } catch (_: KeyPermanentlyInvalidatedException) {
            showSnackbar(
                message = "Biometric modification detected. Please re-enable the feature.",
                action = "Disable",
                onActionClick = { onInvalidKey() },
            )
            null
        } catch (e: Exception) {
            e.printStackTrace()
            showSnackbar(
                message = e.localizedMessage
                    ?: e.message
                    ?: "Unknown error"
            )
            null
        }
    }

    private fun Context.checkBiometricAvailability(): Boolean {
        val result = BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        when (result) {
            BiometricManager.BIOMETRIC_SUCCESS ->
                return true

            BiometricManager.BIOMETRIC_STATUS_UNKNOWN ->
                showSnackbar(message = "Something went wrong with the biometric sensor.")

            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED ->
                showSnackbar(message = "Biometric authentication isn't supported on this device.")

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                showSnackbar(message = "Biometric sensor is currently unavailable.")

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                showSnackbar(message = "No biometric credentials are enrolled.")

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                showSnackbar(message = "This device doesn't have a biometric sensor.")

            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                showSnackbar(message = "Security update is required to use this feature.")

            BiometricManager.BIOMETRIC_ERROR_IDENTITY_CHECK_NOT_ACTIVE ->
                showSnackbar(message = "Identity check is currently inactive.")
        }
        return false
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    @Suppress("SameParameterValue")
    private fun showSnackbar(message: String, action: String, onActionClick: () -> Unit) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).apply {
            setAction(action) { onActionClick() }
        }.show()
    }
}
