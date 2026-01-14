package dev.akexorcist.biometric.pratical.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.akexorcist.biometric.pratical.shared.data.AuthenticationDataRepository

class ViewModelFactory(private val authenticationDataRepository: AuthenticationDataRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(authenticationDataRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
