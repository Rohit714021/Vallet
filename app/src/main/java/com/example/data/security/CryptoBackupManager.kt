package com.example.data.security

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.local.ObsidianDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

sealed class BackupResult {
  data class Success(val fileName: String, val sizeBytes: Long) : BackupResult()
  data class Failure(val message: String) : BackupResult()
}

sealed class RestoreResult {
  data class Success(val sizeBytes: Long, val fileName: String) : RestoreResult()
  data class Failure(val message: String) : RestoreResult()
}

class CryptoBackupManager(private val context: Context) {

  companion object {
    private val MAGIC_HEADER = byteArrayOf('O'.code.toByte(), 'V'.code.toByte(), 'D'.code.toByte(), 'B'.code.toByte(), 1)
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val GCM_TAG_BITS = 128
    private const val PBKDF2_ITERATIONS = 150_000
    private const val KEY_LENGTH_BITS = 256
    private val SQLITE_MAGIC = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
  }

  suspend fun exportEncryptedDatabase(
    targetUri: Uri,
    passphrase: String,
    database: ObsidianDatabase,
  ): BackupResult = withContext(Dispatchers.IO) {
    if (passphrase.length < 4) {
      return@withContext BackupResult.Failure("Passphrase must be at least 4 characters.")
    }
    try {
      database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { cursor ->
        cursor.moveToFirst()
      }

      val dbFile = context.getDatabasePath(ObsidianDatabase.DATABASE_NAME)
      if (!dbFile.exists()) {
        return@withContext BackupResult.Failure("Local database file not found.")
      }
      val rawDbBytes = dbFile.readBytes()

      val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
      val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }
      val key = deriveKey(passphrase, salt)

      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
      cipher.updateAAD(MAGIC_HEADER)
      val cipherBytes = cipher.doFinal(rawDbBytes)

      val outputStream = context.contentResolver.openOutputStream(targetUri, "wt")
        ?: return@withContext BackupResult.Failure("Unable to open destination file in Drive/Storage.")

      outputStream.use { out ->
        out.write(MAGIC_HEADER)
        out.write(salt)
        out.write(iv)
        out.write(cipherBytes)
        out.flush()
      }

      val totalBytes = (MAGIC_HEADER.size + SALT_LENGTH + IV_LENGTH + cipherBytes.size).toLong()
      val name = resolveDisplayName(targetUri) ?: "ObsidianVault_DB.enc"
      BackupResult.Success(fileName = name, sizeBytes = totalBytes)
    } catch (e: Exception) {
      BackupResult.Failure(e.localizedMessage ?: "Failed to encrypt and save database.")
    }
  }

  suspend fun restoreEncryptedDatabase(
    sourceUri: Uri,
    passphrase: String,
    onBeforeSwapCloseDb: () -> Unit,
  ): RestoreResult = withContext(Dispatchers.IO) {
    if (passphrase.isEmpty()) {
      return@withContext RestoreResult.Failure("Enter your vault encryption passphrase.")
    }
    try {
      val inputStream = context.contentResolver.openInputStream(sourceUri)
        ?: return@withContext RestoreResult.Failure("Unable to read selected backup file.")

      val envelope = inputStream.use { it.readBytes() }
      val minSize = MAGIC_HEADER.size + SALT_LENGTH + IV_LENGTH + 32
      if (envelope.size < minSize) {
        return@withContext RestoreResult.Failure("Selected file is not a valid Obsidian Vault backup.")
      }

      val header = envelope.copyOfRange(0, MAGIC_HEADER.size)
      if (!header.contentEquals(MAGIC_HEADER)) {
        return@withContext RestoreResult.Failure("Unrecognized backup header. Select a .enc file exported by Obsidian Vault.")
      }

      val saltStart = MAGIC_HEADER.size
      val ivStart = saltStart + SALT_LENGTH
      val cipherStart = ivStart + IV_LENGTH

      val salt = envelope.copyOfRange(saltStart, ivStart)
      val iv = envelope.copyOfRange(ivStart, cipherStart)
      val cipherBytes = envelope.copyOfRange(cipherStart, envelope.size)

      val key = deriveKey(passphrase, salt)
      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
      cipher.updateAAD(MAGIC_HEADER)

      val decryptedBytes = try {
        cipher.doFinal(cipherBytes)
      } catch (_: Exception) {
        return@withContext RestoreResult.Failure("Incorrect passphrase or corrupted encrypted database.")
      }

      if (decryptedBytes.size < SQLITE_MAGIC.size ||
        !decryptedBytes.copyOfRange(0, SQLITE_MAGIC.size).contentEquals(SQLITE_MAGIC)
      ) {
        return@withContext RestoreResult.Failure("Decrypted payload is not a valid SQLite database.")
      }

      val dbFile = context.getDatabasePath(ObsidianDatabase.DATABASE_NAME)
      val parentDir = dbFile.parentFile ?: context.filesDir
      if (!parentDir.exists()) parentDir.mkdirs()

      val stagingFile = File(parentDir, "${ObsidianDatabase.DATABASE_NAME}.staging")
      stagingFile.writeBytes(decryptedBytes)

      onBeforeSwapCloseDb()

      File(parentDir, "${ObsidianDatabase.DATABASE_NAME}-wal").delete()
      File(parentDir, "${ObsidianDatabase.DATABASE_NAME}-shm").delete()
      if (dbFile.exists()) dbFile.delete()
      if (!stagingFile.renameTo(dbFile)) {
        dbFile.writeBytes(decryptedBytes)
        stagingFile.delete()
      }

      val fileName = resolveDisplayName(sourceUri) ?: "ObsidianVault_DB.enc"
      RestoreResult.Success(sizeBytes = decryptedBytes.size.toLong(), fileName = fileName)
    } catch (e: Exception) {
      RestoreResult.Failure(e.localizedMessage ?: "Restore failed unexpectedly.")
    }
  }

  private fun deriveKey(passphrase: String, salt: ByteArray): SecretKeySpec {
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val spec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
    val secret = factory.generateSecret(spec)
    return SecretKeySpec(secret.encoded, "AES")
  }

  private fun resolveDisplayName(uri: Uri): String? {
    return runCatching {
      context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
          if (cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) cursor.getString(idx) else null
          } else null
        }
    }.getOrNull()
  }
}
