package com.camilo.usersmanagement.data

import android.util.Base64
import org.json.JSONObject

data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val status: String
) {
    companion object {
        fun fromJson(json: JSONObject) = User(
            id = json.optString("id"),
            name = json.optString("name"),
            email = json.optString("email"),
            role = json.optString("role"),
            status = json.optString("status")
        )
    }
}

/** Valores de los enums UserRole y UserStatus del backend. */
val ROLES = listOf("MEMBER", "REVIEWER", "ADMIN")
val STATUSES = listOf("ACTIVE", "PENDING", "INACTIVE", "BLOCKED")

/** Datos que vienen dentro del JWT: sub = id del usuario, role, exp (segundos). */
data class Session(val token: String, val userId: String, val role: String, val expiresAtMs: Long) {
    val isAdmin get() = role == "ADMIN"
    val canList get() = role == "ADMIN" || role == "REVIEWER"

    companion object {
        fun fromToken(token: String): Session? = runCatching {
            val payload = token.split(".")[1]
            val decoded = String(
                Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP),
                Charsets.UTF_8
            )
            val json = JSONObject(decoded)
            Session(
                token = token,
                userId = json.optString("sub"),
                role = json.optString("role"),
                expiresAtMs = json.optLong("exp") * 1000
            )
        }.getOrNull()
    }
}
