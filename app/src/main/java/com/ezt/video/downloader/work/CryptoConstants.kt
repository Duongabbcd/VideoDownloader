package com.ezt.video.downloader.work

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import java.io.IOException
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
        context: Context
    ) : File {
        val secretKey = getAESKey(context)
            ?: throw IllegalStateException("AES key not found")

        val tempFile = File.createTempFile("decrypted_", ".tmp", context.cacheDir)
        tempFile.deleteOnExit()

        inputFile.inputStream().use { input ->
            val header = ByteArray(4)
            if (input.read(header) != header.size) {
                throw IOException("Failed to read header")
            }

            val magic = String(header)
            if (magic != HEADER_MAGIC) {
                throw IllegalArgumentException("Invalid file format")
            }

            val version = input.read().toByte()
            if (version != HEADER_VERSION) {
                throw IllegalArgumentException("Unsupported version")
            }

            val iv = ByteArray(IV_SIZE)
            if (input.read(iv) != iv.size) {
                throw IOException("Failed to read IV")
            }

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(TAG_SIZE_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            CipherInputStream(input, cipher).use { cipherIn ->
                tempFile.outputStream().use { out ->
                    cipherIn.copyTo(out)
                }
            }
        }

        return tempFile
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

    fun isFileEncryptedByUs(file: File): Boolean {
        if (!file.exists() || file.length() < 17) return false

        val expectedMagic = HEADER_MAGIC.toByteArray()
        val expectedVersion = HEADER_VERSION

        file.inputStream().use { input ->
            val header = ByteArray(4)
            if (input.read(header) != header.size) return false

            val magic = String(header)
            if (magic != HEADER_MAGIC) return false

            val version = input.read()
            if (version != expectedVersion.toInt()) return false

            // Optionally check IV size too:
            val iv = ByteArray(IV_SIZE)
            if (input.read(iv) != IV_SIZE) return false

            return true
        }
    }

}