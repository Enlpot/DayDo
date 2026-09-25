/*
 * Copyright (C) 2026  Enlpot
 *
 * WebDAV 密码 AES-GCM 加密（密钥存于 Android Keystore，不可导出）
 */
package com.enlpot.daydo.core.data.datastore

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 用 Android Keystore 中的 AES 密钥对 WebDAV 密码做 GCM 加密。
 * 存储格式："Base64(iv):Base64(密文)"；无法解密的旧明文在读取时原样返回，保存时自动迁移为密文。
 */
object WebDavCipher {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "daydo_webdav_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128

    private fun getOrCreateKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return runCatching {
            val generator =
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            generator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            generator.generateKey()
        }.getOrNull()
    }

    /** 返回加密串；明文为空或加密失败时返回 null（调用方按原值存储） */
    fun encrypt(plain: String): String? {
        if (plain.isEmpty()) return null
        val key = getOrCreateKey() ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(iv, Base64.NO_WRAP) +
                ":" +
                Base64.encodeToString(cipherText, Base64.NO_WRAP)
        }.getOrNull()
    }

    /** 解密；非加密格式（旧明文）或解密失败时返回 null */
    fun decrypt(encoded: String?): String? {
        if (encoded.isNullOrEmpty()) return null
        val parts = encoded.split(":", limit = 2)
        if (parts.size != 2) return null
        val key = getOrCreateKey() ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                key,
                GCMParameterSpec(GCM_TAG_BITS, Base64.decode(parts[0], Base64.NO_WRAP)),
            )
            String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), Charsets.UTF_8)
        }.getOrNull()
    }
}
