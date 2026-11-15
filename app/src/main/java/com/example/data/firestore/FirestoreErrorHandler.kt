package com.example.data.firestore

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Throwable, operationType: OperationType, path: String?): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

object AlapFirestoreProvider {
    @Volatile
    private var configuredDbId: String? = null

    fun getDatabase(context: Context): FirebaseFirestore {
        val appContext = context.applicationContext
        val dbId = appContext.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(dbId)
        if (configuredDbId != dbId) {
            synchronized(this) {
                if (configuredDbId != dbId) {
                    try {
                        val cacheSettings = PersistentCacheSettings.newBuilder()
                            .setSizeBytes(100L * 1024L * 1024L) // 100 MB offline disk cache
                            .build()
                        db.firestoreSettings = FirebaseFirestoreSettings.Builder()
                            .setLocalCacheSettings(cacheSettings)
                            .build()
                    } catch (e: IllegalStateException) {
                        // Settings already initialized on this instance
                        Log.d("AlapFirestoreProvider", "Firestore settings already configured")
                    }
                    configuredDbId = dbId
                }
            }
        }
        return db
    }
}
