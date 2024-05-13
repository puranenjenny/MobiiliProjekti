package com.example.mobiiliprojekti.services

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
class SecurityManager {

    // Generate salt for password
    fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt
    }

    // add salt and hash password
    fun hashPassword(password: String, salt: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        val hashedPassword = md.digest(password.toByteArray())
        return Base64.getEncoder().encodeToString(hashedPassword)
    }
}