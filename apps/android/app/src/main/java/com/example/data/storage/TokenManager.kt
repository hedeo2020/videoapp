package com.example.data.storage

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID

class TokenManager(context: Context) {
    var isUsingPlaintextFallback: Boolean = false
        private set

    private val prefs = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "secure_stream_encrypted_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e("TokenManager", "Failed to initialize EncryptedSharedPreferences, falling back", e)
        isUsingPlaintextFallback = true
        context.getSharedPreferences("secure_stream_prefs", Context.MODE_PRIVATE)
    }

    fun getAccessToken(): String? = prefs.getString("access_token", null)

    fun setAccessToken(token: String?) {
        prefs.edit().putString("access_token", token).apply()
    }

    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)

    fun setRefreshToken(token: String?) {
        prefs.edit().putString("refresh_token", token).apply()
    }

    fun getDeviceId(): String {
        var deviceId = prefs.getString("device_id", null)
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            prefs.edit().putString("device_id", deviceId).apply()
        }
        return deviceId
    }

    fun getUserEmail(): String? = prefs.getString("user_email", null)
    fun setUserEmail(email: String?) = prefs.edit().putString("user_email", email).apply()

    fun getUserName(): String? = prefs.getString("user_name", null)
    fun setUserName(name: String?) = prefs.edit().putString("user_name", name).apply()

    fun getUserId(): String? = prefs.getString("user_id", null)
    fun setUserId(id: String?) = prefs.edit().putString("user_id", id).apply()

    fun getUserRole(): String? = prefs.getString("user_role", null)
    fun setUserRole(role: String?) = prefs.edit().putString("user_role", role).apply()

    var onUnauthorizedCallback: (() -> Unit)? = null

    fun isLoggedIn(): Boolean {
        return getAccessToken() != null && getRefreshToken() != null
    }

    fun clear(notify: Boolean = true) {
        prefs.edit()
            .remove("access_token")
            .remove("refresh_token")
            .remove("user_id")
            .remove("user_email")
            .remove("user_name")
            .remove("user_role")
            .apply()
        if (notify) {
            onUnauthorizedCallback?.invoke()
        }
    }
}
