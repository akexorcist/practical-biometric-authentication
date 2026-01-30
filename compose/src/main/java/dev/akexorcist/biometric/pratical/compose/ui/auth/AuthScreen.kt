package dev.akexorcist.biometric.pratical.compose.ui.auth

import android.content.Context
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.util.Base64
import androidx.biometric.AuthenticationRequest
import androidx.biometric.AuthenticationResult
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.compose.rememberAuthenticationLauncher
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import dev.akexorcist.biometric.pratical.shared.crypto.CryptographyManager
import dev.akexorcist.biometric.pratical.compose.ui.navigation.AppScreen
import dev.akexorcist.biometric.pratical.compose.ui.theme.PracticalBiometricAuthenticationTheme
import dev.akexorcist.biometric.pratical.shared.viewmodel.AuthUiState
import dev.akexorcist.biometric.pratical.shared.viewmodel.AuthViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.crypto.Cipher

@Composable
fun AuthRoute(
    navController: NavController,
    cryptographyManager: CryptographyManager,
    viewModel: AuthViewModel,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val authenticationLauncher = rememberAuthenticationLauncher { result ->
        when (result) {
            is AuthenticationResult.Success -> {
                when (val uiState = uiState) {
                    is AuthUiState.Initial -> {
                        result.crypto?.cipher?.let { cipher ->
                            val encryptedData = cryptographyManager.encrypt(uiState.token.toByteArray(), cipher)
                            val encryptedToken = Base64.encodeToString(encryptedData.first, Base64.NO_WRAP)
                            val iv = Base64.encodeToString(encryptedData.second, Base64.NO_WRAP)
                            viewModel.onEnableBiometricSuccess(encryptedToken, iv)
                        }
                    }

                    is AuthUiState.BiometricEnabled -> {
                        result.crypto?.cipher?.let { cipher ->
                            val encryptedToken = Base64.decode(uiState.encryptedData.encryptedToken, Base64.NO_WRAP)
                            val decryptedToken = cryptographyManager.decrypt(encryptedToken, cipher)
                            navController.navigate(AppScreen.Home(String(decryptedToken)))
                        }
                    }
                }
            }

            is AuthenticationResult.Error -> {
                val errorCode = result.errorCode
                val message = result.errString
                if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON && errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                    scope.launch {
                        snackbarHostState.showSnackbar(message.toString())
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        AuthScreen(
            modifier = Modifier.padding(innerPadding),
            uiState = uiState,
            onEnableBiometricClick = {
                val canAuthenticate = context.checkBiometricAvailability(
                    scope = scope,
                    snackbarHostState = snackbarHostState,
                )
                if (canAuthenticate) {
                    val cipher = handleCipherResult(
                        scope = scope,
                        snackbarHostState = snackbarHostState,
                        onInvalidKey = {
                            runCatching { cryptographyManager.deleteKey() }
                            viewModel.onDisableBiometricClick()
                        },
                    ) {
                        cryptographyManager.getCipherForEncryption()
                    } ?: return@AuthScreen
                    val cryptoObject = BiometricPrompt.CryptoObject(cipher)
                    authenticationLauncher.launch(
                        AuthenticationRequest.biometricRequest(
                            title = "Enable Biometric Authentication",
                            authFallback = AuthenticationRequest.Biometric.Fallback.NegativeButton(negativeButtonText = "Cancel"),
                            init = {
                                setSubtitle("Confirm to enable biometric authentication")
                                setIsConfirmationRequired(true)
                                setMinStrength(AuthenticationRequest.Biometric.Strength.Class3(cryptoObject))
                            },
                        )
                    )
                }
            },
            onDisableBiometricClick = {
                runCatching { cryptographyManager.deleteKey() }
                viewModel.onDisableBiometricClick()
            },
            onAuthenticateClick = {
                val uiState = uiState
                if (uiState is AuthUiState.BiometricEnabled) {
                    val canAuthenticate = context.checkBiometricAvailability(
                        scope = scope,
                        snackbarHostState = snackbarHostState,
                    )
                    if (canAuthenticate) {
                        val iv = Base64.decode(uiState.encryptedData.iv, Base64.NO_WRAP)
                        val cipher = handleCipherResult(
                            scope = scope,
                            snackbarHostState = snackbarHostState,
                            onInvalidKey = {
                                runCatching { cryptographyManager.deleteKey() }
                                viewModel.onDisableBiometricClick()
                            },
                        ) {
                            cryptographyManager.getCipherForDecryption(iv)
                        } ?: return@AuthScreen
                        val cryptoObject = BiometricPrompt.CryptoObject(cipher)
                        authenticationLauncher.launch(
                            AuthenticationRequest.biometricRequest(
                                title = "Biometric Authentication",
                                authFallback = AuthenticationRequest.Biometric.Fallback.NegativeButton(negativeButtonText = "Cancel"),
                                init = {
                                    setSubtitle("Authenticate to log in")
                                    setIsConfirmationRequired(true)
                                    setMinStrength(AuthenticationRequest.Biometric.Strength.Class3(cryptoObject))
                                },
                            )
                        )
                    }
                }
            },
            onRandomizeTokenClick = {
                viewModel.onRandomizeTokenClick()
            }
        )
    }
}

@Composable
fun AuthScreen(
    modifier: Modifier = Modifier,
    uiState: AuthUiState,
    onEnableBiometricClick: () -> Unit,
    onDisableBiometricClick: () -> Unit,
    onAuthenticateClick: () -> Unit,
    onRandomizeTokenClick: () -> Unit,
) {
    val isBiometricEnabled = uiState is AuthUiState.BiometricEnabled

    Surface(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "Token to encrypt",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiState.token,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onRandomizeTokenClick,
                    enabled = !isBiometricEnabled,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                    )
                ) {
                    Text(text = "Randomize Token")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onEnableBiometricClick,
                    enabled = !isBiometricEnabled,
                ) {
                    Text(text = "Enable Biometric")
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onAuthenticateClick,
                    enabled = isBiometricEnabled,
                ) {
                    Text(text = "Authenticate")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDisableBiometricClick,
                    enabled = isBiometricEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(text = "Disable Biometric")
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

private fun handleCipherResult(
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onInvalidKey: () -> Unit,
    block: () -> Cipher,
): Cipher? {
    return try {
        block()
    } catch (_: KeyPermanentlyInvalidatedException) {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Biometric modification detected. Please re-enable the feature.",
                actionLabel = "Disable",
            )
            when (result) {
                SnackbarResult.ActionPerformed -> {
                    onInvalidKey()
                }

                SnackbarResult.Dismissed -> Unit
            }
        }
        null
    } catch (e: Exception) {
        scope.launch {
            snackbarHostState.showSnackbar(
                message = e.localizedMessage
                    ?: e.message
                    ?: "Unknown error"
            )
        }
        null
    }
}

private fun Context.checkBiometricAvailability(
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
): Boolean {
    val result = BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    when (result) {
        BiometricManager.BIOMETRIC_SUCCESS ->
            return true

        BiometricManager.BIOMETRIC_STATUS_UNKNOWN ->
            scope.launch { snackbarHostState.showSnackbar(message = "Something went wrong with the biometric sensor.") }

        BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED ->
            scope.launch { snackbarHostState.showSnackbar(message = "Biometric authentication isn't supported on this device.") }

        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
            scope.launch { snackbarHostState.showSnackbar(message = "Biometric sensor is currently unavailable.") }

        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
            scope.launch { snackbarHostState.showSnackbar(message = "No biometric credentials are enrolled.") }

        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
            scope.launch { snackbarHostState.showSnackbar(message = "This device doesn't have a biometric sensor.") }

        BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
            scope.launch { snackbarHostState.showSnackbar(message = "Security update is required to use this feature.") }

        BiometricManager.BIOMETRIC_ERROR_IDENTITY_CHECK_NOT_ACTIVE ->
            scope.launch { snackbarHostState.showSnackbar(message = "Identity check is currently inactive.") }
    }
    return false
}

@Preview
@Composable
private fun AuthScreenPreview() {
    PracticalBiometricAuthenticationTheme {
        AuthScreen(
            uiState = AuthUiState.Initial(token = "12345-12345-12345-12345"),
            onEnableBiometricClick = {},
            onDisableBiometricClick = {},
            onAuthenticateClick = {},
            onRandomizeTokenClick = {},
        )
    }
}
