package dev.akexorcist.biometric.pratical.compose.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.akexorcist.biometric.pratical.compose.ui.auth.AuthRoute
import dev.akexorcist.biometric.pratical.compose.ui.home.HomeScreen
import dev.akexorcist.biometric.pratical.compose.ui.navigation.AppScreen
import dev.akexorcist.biometric.pratical.compose.ui.theme.PracticalBiometricAuthenticationTheme
import dev.akexorcist.biometric.pratical.shared.crypto.CryptographyManager
import dev.akexorcist.biometric.pratical.shared.data.AuthenticationDataRepository
import dev.akexorcist.biometric.pratical.shared.viewmodel.AuthViewModel
import dev.akexorcist.biometric.pratical.shared.viewmodel.ViewModelFactory

class MainActivity : FragmentActivity() {
    private val cryptographyManager = CryptographyManager()
    private val authenticationDataRepository by lazy { AuthenticationDataRepository(this) }
    private val authViewModel: AuthViewModel by viewModels { ViewModelFactory(authenticationDataRepository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            PracticalBiometricAuthenticationTheme {
                NavHost(
                    navController = navController,
                    startDestination = AppScreen.Auth,
                    enterTransition = { fadeIn(animationSpec = tween(200)) },
                    exitTransition = { fadeOut(animationSpec = tween(200)) },
                ) {
                    composable<AppScreen.Auth> {
                        AuthRoute(
                            navController = navController,
                            cryptographyManager = cryptographyManager,
                            viewModel = authViewModel
                        )
                    }
                    composable<AppScreen.Home> { backStackEntry ->
                        val decryptedToken = backStackEntry.toRoute<AppScreen.Home>().decryptedToken
                        HomeScreen(
                            navController = navController,
                            decryptedToken = decryptedToken,
                        )
                    }
                }
            }
        }
    }
}
