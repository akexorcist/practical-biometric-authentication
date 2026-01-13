
package dev.akexorcist.biometric.pratical.compose.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.akexorcist.biometric.pratical.crypto.CryptographyManager
import dev.akexorcist.biometric.pratical.data.AuthenticationDataRepository
import dev.akexorcist.biometric.pratical.compose.ui.auth.AuthRoute
import dev.akexorcist.biometric.pratical.compose.ui.auth.AuthViewModel
import dev.akexorcist.biometric.pratical.compose.ui.home.HomeScreen
import dev.akexorcist.biometric.pratical.compose.ui.navigation.AppScreen
import dev.akexorcist.biometric.pratical.compose.ui.theme.PracticalBiometricAuthenticationTheme

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
                NavHost(navController = navController, startDestination = AppScreen.Auth) {
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
