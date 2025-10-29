package com.ezt.priv.shortvideodownloader.work

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoConstants {
    const val HEADER_MAGIC = "ENCR" // 4 bytes
    const val HEADER_VERSION: Byte = 1
    const val IV_SIZE = 12
    const val TAG_SIZE_BITS = 128 // AES-GCM tag size
    const val AES_KEY_SIZE = 32 // 256 bits

    fun encryptMediaHeader(
        inputFile: File,
        outputFile: File,
        context: Context,
        headerSize: Int = 4096
    ) {
        val secretKey = getAESKey(context) ?: throw IllegalStateException("AES key not found")
        val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        inputFile.inputStream().use { input ->
            outputFile.outputStream().use { output ->
                // Write custom header
                output.write(HEADER_MAGIC.toByteArray()) // e.g., "ENCH"
                output.write(byteArrayOf(HEADER_VERSION))
                output.write(iv)

                val headerBytes = ByteArray(headerSize)
                val bytesRead = input.read(headerBytes)
                val encryptedHeader = cipher.doFinal(headerBytes, 0, bytesRead)

                // Write encrypted header
                output.write(encryptedHeader)

                // Write the rest of the file unmodified
                input.copyTo(output)
            }
        }
    }


    fun decryptMediaHeader(
        encryptedFile: File,
        context: Context,
        headerSize: Int = 4096
    ): File {
        val secretKey = getAESKey(context) ?: throw IllegalStateException("AES key not found")
        val tempFile = File.createTempFile("decrypted_", ".media", context.cacheDir)
        tempFile.deleteOnExit()

        encryptedFile.inputStream().use { input ->
            val headerMagic = ByteArray(4)
            if (input.read(headerMagic) != 4 || String(headerMagic) != HEADER_MAGIC)
                throw IllegalArgumentException("Invalid file format")

            val version = input.read()
            if (version != HEADER_VERSION.toInt())
                throw IllegalArgumentException("Unsupported version")

            val iv = ByteArray(12)
            input.read(iv)

            val encryptedHeader = ByteArray(headerSize + 16) // 16 bytes for GCM tag
            input.read(encryptedHeader)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            val decryptedHeader = cipher.doFinal(encryptedHeader)

            tempFile.outputStream().use { out ->
                out.write(decryptedHeader)

                // Write the rest of the file (already plaintext)
                input.copyTo(out)
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

        val aesKeyBase64 = "r7/HrnEYlxebrdKS/tItXyFbI5rFXoHik3xlmAOAmPs="
        println("getAESKey: $aesKeyBase64")
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