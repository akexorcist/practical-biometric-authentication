package dev.akexorcist.biometric.pratical.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "authentication")

class AuthenticationDataRepository(
    private val context: Context
) {
    private val encryptedTokenKey = stringPreferencesKey("encrypted_token")
    private val ivKey = stringPreferencesKey("iv")

    data class EncryptedAuthData(
        val encryptedToken: String,
        val iv: String,
    )

    fun getEncryptedAuthData(): Flow<EncryptedAuthData?> = context.dataStore.data
        .map { preferences ->
            val encryptedToken = preferences[encryptedTokenKey]
            val iv = preferences[ivKey]
            if (encryptedToken != null && iv != null) {
                EncryptedAuthData(encryptedToken, iv)
            } else {
                null
            }
        }

    fun biometricAuthenticationEnabled(): Boolean = runBlocking {
        context.dataStore.data.map { preferences ->
            preferences.contains(encryptedTokenKey) && preferences.contains(ivKey)
        }.first()
    }

    suspend fun saveEncryptedAuthData(encryptedToken: String, iv: String) {
        context.dataStore.edit { preferences ->
            preferences[encryptedTokenKey] = encryptedToken
            preferences[ivKey] = iv
        }
    }

    suspend fun clearEncryptedAuthData() {
        context.dataStore.edit { preferences ->
            preferences.remove(encryptedTokenKey)
            preferences.remove(ivKey)
        }
    }
}
