package dev.akexorcist.biometric.pratical.compose.ui.auth

import android.util.Base64
import androidx.biometric.AuthenticationRequest
import androidx.biometric.AuthenticationResult
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import dev.akexorcist.biometric.pratical.crypto.CryptographyManager
import dev.akexorcist.biometric.pratical.compose.ui.navigation.AppScreen
import dev.akexorcist.biometric.pratical.compose.ui.theme.PracticalBiometricAuthenticationTheme
import kotlinx.coroutines.launch

@Composable
fun AuthRoute(
    navController: NavController,
    cryptographyManager: CryptographyManager,
    viewModel: AuthViewModel,
) {
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
                val cryptoObject = cryptographyManager.getCryptoObjectForEncryption()
                authenticationLauncher.launch(
                    AuthenticationRequest.biometricRequest(
                        title = "Enable Biometric Authentication",
                        authFallback = AuthenticationRequest.Biometric.Fallback.NegativeButton(negativeButtonText = "Cancel"),
                        init = {
                            setSubtitle("Confirm to enable biometric authentication")
                            setIsConfirmationRequired(true)
                            setMinStrength(AuthenticationRequest.Biometric.Strength.Class3(cryptoObject = cryptoObject))
                        },
                    )
                )
            },
            onDisableBiometricClick = {
                viewModel.onDisableBiometricClick(cryptographyManager)
            },
            onAuthenticateClick = {
                val uiState = uiState
                if (uiState is AuthUiState.BiometricEnabled) {
                    val iv = Base64.decode(uiState.encryptedData.iv, Base64.NO_WRAP)
                    val cryptoObject = cryptographyManager.getCryptoObjectForDecryption(iv)
                    authenticationLauncher.launch(
                        AuthenticationRequest.biometricRequest(
                            title = "Authenticate",
                            authFallback = AuthenticationRequest.Biometric.Fallback.NegativeButton(negativeButtonText = "Cancel"),
                            init = {
                                setSubtitle("Authenticate to log in")
                                setIsConfirmationRequired(true)
                                setMinStrength(AuthenticationRequest.Biometric.Strength.Class3(cryptoObject = cryptoObject))
                            },
                        )
                    )
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
