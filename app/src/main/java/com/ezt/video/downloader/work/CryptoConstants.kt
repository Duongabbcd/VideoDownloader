package com.ezt.video.downloader.work

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoConstants {
    const val HEADER_MAGIC = "ENCR" // 4 bytes
    const val HEADER_VERSION: Byte = 1
    const val IV_SIZE = 12
    const val TAG_SIZE_BITS = 128 // AES-GCM tag size
    const val AES_KEY_SIZE = 32 // 256 bits

    fun encryptFile(
        inputFile: File,
        outputFile: File,
        context: Context
    ) {
        val iv = ByteArray(IV_SIZE).apply {
            SecureRandom().nextBytes(this)
        }

        val secretKey = getAESKey(context) ?: return

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(TAG_SIZE_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        outputFile.outputStream().use { output ->
            output.write(HEADER_MAGIC.toByteArray())
            output.write(byteArrayOf(HEADER_VERSION))
            output.write(iv)

            CipherOutputStream(output, cipher).use { cipherOut ->
                inputFile.inputStream().copyTo(cipherOut)
            }
        }
    }

    fun decryptFile(
        inputFile: File,
        outputFile: File,
        context: Context
    ) {
        val secretKey = getAESKey(context) ?: return

        inputFile.inputStream().use { input ->
            // 1. Read and validate header
            val header = ByteArray(4)
            input.read(header)
            val magic = String(header)
            if (magic != CryptoConstants.HEADER_MAGIC) {
                throw IllegalArgumentException("Invalid file format")
            }

            val version = input.read().toByte()
            if (version != CryptoConstants.HEADER_VERSION) {
                throw IllegalArgumentException("Unsupported version")
            }

            val iv = ByteArray(CryptoConstants.IV_SIZE)
            input.read(iv)

            // 2. Setup cipher for decryption
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(CryptoConstants.TAG_SIZE_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            // 3. Decrypt and write to output
            CipherInputStream(input, cipher).use { cipherIn ->
                outputFile.outputStream().use { out ->
                    cipherIn.copyTo(out)
                }
            }
        }
    }

    fun createAESKey(context: Context) {
        val masterKeyAlias = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKeyAlias,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

// Store AES key (as Base64)
        val aesKey = generateRandomAESKey()
        println("createAESKey: $aesKey")
        val aesKeyBase64 = Base64.encodeToString(aesKey.encoded, Base64.DEFAULT)

        sharedPreferences.edit().putString("aes_key", aesKeyBase64).apply()
    }

    private fun generateRandomAESKey(): SecretKey {
        val keyBytes = ByteArray(AES_KEY_SIZE)
        SecureRandom().nextBytes(keyBytes)
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun getAESKey(context: Context): SecretKey? {
        val masterKeyAlias = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKeyAlias,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val aesKeyBase64 = sharedPreferences.getString("aes_key", null) ?: return null

        val decodedKey = Base64.decode(aesKeyBase64, Base64.DEFAULT)
        return SecretKeySpec(decodedKey, 0, decodedKey.size, "AES")
    }


}