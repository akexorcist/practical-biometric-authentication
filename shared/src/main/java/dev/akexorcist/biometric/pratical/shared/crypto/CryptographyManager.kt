package dev.akexorcist.biometric.pratical.shared.crypto

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val KEY_NAME = "biometric_key"
private const val KEYSTORE_TYPE = "AndroidKeyStore"
private const val KEY_SIZE = 256
private const val ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
private const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
private const val ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
private const val GCM_AUTHENTICATION_TAG_LENGTH = 128

class CryptographyManager {
    private val keyStore = KeyStore.getInstance(KEYSTORE_TYPE).apply {
        load(null)
    }

    fun getCipherForEncryption(): Cipher {
        val secretKey = getOrCreateSecretKey()
        val cipher = getCipher()
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher
    }

    fun getCipherForDecryption(iv: ByteArray): Cipher {
        val secretKey = getOrCreateSecretKey()
        val cipher = getCipher()
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_AUTHENTICATION_TAG_LENGTH, iv))
        return cipher
    }

    fun encrypt(data: ByteArray, cipher: Cipher): Pair<ByteArray, ByteArray> {
        val encryptedData = cipher.doFinal(data)
        val iv = cipher.iv
        return Pair(encryptedData, iv)
    }

    fun decrypt(encryptedData: ByteArray, cipher: Cipher): ByteArray {
        return cipher.doFinal(encryptedData)
    }

    fun deleteKey() {
        keyStore.deleteEntry(KEY_NAME)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        return (keyStore.getKey(KEY_NAME, null) as? SecretKey) ?: generateSecretKey()
    }

    private fun generateSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM, KEYSTORE_TYPE)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(KEY_NAME, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(ENCRYPTION_BLOCK_MODE)
            .setKeySize(KEY_SIZE)
            .setEncryptionPaddings(ENCRYPTION_PADDING)
            .setUserAuthenticationRequired(true)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    setInvalidatedByBiometricEnrollment(true)
                }
            }
            .build()
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    private fun getCipher(): Cipher {
        val transformation = "$ENCRYPTION_ALGORITHM/$ENCRYPTION_BLOCK_MODE/$ENCRYPTION_PADDING"
        return Cipher.getInstance(transformation)
    }
}
