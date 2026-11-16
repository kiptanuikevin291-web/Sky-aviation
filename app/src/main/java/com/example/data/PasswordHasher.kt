package com.example.data

import java.security.MessageDigest
import java.security.SecureRandom

object PasswordHasher {
  private val random = SecureRandom()

  fun generateSaltHex(): String {
    val bytes = ByteArray(16)
    random.nextBytes(bytes)
    return bytes.joinToString("") { "%02x".format(it) }
  }

  fun hashPassword(password: String, saltHex: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val input = "$saltHex:$password".toByteArray(Charsets.UTF_8)
    val hashed = digest.digest(input)
    return hashed.joinToString("") { "%02x".format(it) }
  }

  fun verify(password: String, saltHex: String, expectedHash: String): Boolean {
    val candidate = hashPassword(password, saltHex)
    return MessageDigest.isEqual(
      candidate.toByteArray(Charsets.UTF_8),
      expectedHash.toByteArray(Charsets.UTF_8),
    )
  }

  fun normalizePhone(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    return when {
      digits.startsWith("254") && digits.length >= 12 -> "0${digits.substring(3)}"
      else -> digits
    }
  }
}
