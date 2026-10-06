package es.routerapp.app

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

interface CredentialStore {
    fun getSavedPassword(): String?
    fun savePassword(password: String)
    fun clear()
    fun hasSavedPassword(): Boolean
}

@Suppress("DEPRECATION")
class SecureCredentialStore(
    context: Context,
    prefsName: String = PREFS_NAME,
) : CredentialStore {

    private val prefs: SharedPreferences? = runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            prefsName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }.onFailure { e ->
        Log.e(TAG, "Failed to initialize EncryptedSharedPreferences, resetting storage", e)
        runCatching {
            context.deleteSharedPreferences(prefsName)
        }
    }.getOrNull()

    override fun getSavedPassword(): String? {
        return runCatching {
            prefs?.getString(KEY_PASSWORD, null)
        }.getOrNull()
    }

    override fun savePassword(password: String) {
        runCatching {
            prefs?.edit()?.putString(KEY_PASSWORD, password)?.apply()
        }.onFailure { e ->
            Log.e(TAG, "Failed to save password to EncryptedSharedPreferences", e)
        }
    }

    override fun clear() {
        runCatching {
            prefs?.edit()?.remove(KEY_PASSWORD)?.apply()
        }.onFailure { e ->
            Log.e(TAG, "Failed to clear password from EncryptedSharedPreferences", e)
        }
    }

    override fun hasSavedPassword(): Boolean {
        return !getSavedPassword().isNullOrEmpty()
    }

    companion object {
        private const val TAG = "SecureCredentialStore"
        private const val PREFS_NAME = "secure_credentials"
        private const val KEY_PASSWORD = "router_password"
    }
}
