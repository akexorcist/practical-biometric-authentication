
package dev.akexorcist.biometric.pratical.compose.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.akexorcist.biometric.pratical.data.AuthenticationDataRepository
import dev.akexorcist.biometric.pratical.compose.ui.auth.AuthViewModel

class ViewModelFactory(private val authenticationDataRepository: AuthenticationDataRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(authenticationDataRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
