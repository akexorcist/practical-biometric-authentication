package dev.akexorcist.biometric.pratical.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.akexorcist.biometric.pratical.shared.crypto.CryptographyManager
import dev.akexorcist.biometric.pratical.shared.data.AuthenticationDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class AuthViewModel(
    private val authenticationDataRepository: AuthenticationDataRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<AuthUiState>(
        AuthUiState.Initial(
            token = if (authenticationDataRepository.biometricAuthenticationEnabled()) {
                "**Encrypted**"
            } else UUID.randomUUID().toString()
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState

    init {
        viewModelScope.launch {
            authenticationDataRepository.getEncryptedAuthData().first()?.let { (encryptedToken, iv) ->
                _uiState.update {
                    AuthUiState.BiometricEnabled(
                        token = it.token,
                        encryptedData = AuthUiState.BiometricEnabled.EncryptedData(encryptedToken, iv),
                    )
                }
            }
        }
    }

    fun onEnableBiometricSuccess(encryptedToken: String, iv: String) {
        viewModelScope.launch {
            authenticationDataRepository.saveEncryptedAuthData(encryptedToken, iv)
            _uiState.update {
                AuthUiState.BiometricEnabled(
                    token = it.token,
                    encryptedData = AuthUiState.BiometricEnabled.EncryptedData(encryptedToken, iv),
                )
            }
        }
    }

    fun onDisableBiometricClick() {
        viewModelScope.launch {
            authenticationDataRepository.clearEncryptedAuthData()
            _uiState.update {
                val newToken = UUID.randomUUID().toString()
                AuthUiState.Initial(token = newToken)
            }
        }
    }

    fun onRandomizeTokenClick() {
        val newToken = UUID.randomUUID().toString()
        _uiState.update {
            when (it) {
                is AuthUiState.Initial -> it.copy(token = newToken)
                is AuthUiState.BiometricEnabled -> it.copy(token = newToken)
            }
        }
    }
}

sealed class AuthUiState(open val token: String) {
    data class Initial(
        override val token: String,
    ) : AuthUiState(token)

    data class BiometricEnabled(
        override val token: String,
        val encryptedData: EncryptedData,
    ) : AuthUiState(token) {
        data class EncryptedData(
            val encryptedToken: String,
            val iv: String,
        )
    }
}
