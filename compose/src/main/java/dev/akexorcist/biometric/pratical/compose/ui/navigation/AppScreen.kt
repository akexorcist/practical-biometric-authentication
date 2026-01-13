package dev.akexorcist.biometric.pratical.compose.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface AppScreen {
    @Serializable
    data object Auth : AppScreen

    @Serializable
    data class Home(val decryptedToken: String) : AppScreen
}
