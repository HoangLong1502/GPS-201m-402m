package com.longvhse192032.gpsracer.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("gps_racer_session")

class SessionStore(private val context: Context) {
    private val gson = Gson()
    private val userKey = stringPreferencesKey("user")
    private val tokenKey = stringPreferencesKey("token")

    val sessionFlow: Flow<Pair<UserProfile?, String?>> = context.dataStore.data.map { prefs ->
        val user = prefs[userKey]?.let { gson.fromJson(it, UserProfile::class.java) }
        val token = prefs[tokenKey]
        user to token
    }

    suspend fun saveSession(user: UserProfile, token: String) {
        context.dataStore.edit { prefs ->
            prefs[userKey] = gson.toJson(user)
            prefs[tokenKey] = token
        }
        ApiClient.setAuthToken(token)
    }

    suspend fun saveUser(user: UserProfile) {
        context.dataStore.edit { prefs ->
            prefs[userKey] = gson.toJson(user)
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
        ApiClient.setAuthToken(null)
    }
}
