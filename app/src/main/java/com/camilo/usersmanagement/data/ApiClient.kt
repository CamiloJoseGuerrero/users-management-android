package com.camilo.usersmanagement.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Cliente HTTP de la API de usuarios (Spring Boot desplegado en Render).
 * Usa HttpURLConnection y org.json, que vienen incluidos en Android,
 * para no depender de librerias externas.
 */
object ApiClient {

    /**
     * Para probar contra tu PC desde el emulador, cambia por:
     * "http://10.0.2.2:8080"
     */
    const val BASE_URL = "https://users-management-camilo.onrender.com"

    // Render (plan gratis) puede tardar hasta ~90 s en despertar.
    private const val TIMEOUT_MS = 120_000

    suspend fun login(email: String, password: String): String {
        val body = JSONObject().put("email", email).put("password", password)
        val json = request("POST", "/api/auth/login", body = body, token = null)
        return JSONObject(json).getString("accessToken")
    }

    suspend fun getUsers(token: String): List<User> {
        val json = request("GET", "/api/users", body = null, token = token)
        val array = JSONArray(json)
        return (0 until array.length()).map { User.fromJson(array.getJSONObject(it)) }
    }

    suspend fun createUser(user: User, password: String, token: String?): User {
        val body = JSONObject()
            .put("id", user.id)
            .put("name", user.name)
            .put("email", user.email)
            .put("password", password)
            .put("role", user.role)
        return User.fromJson(JSONObject(request("POST", "/api/users", body, token)))
    }

    suspend fun updateUser(user: User, newPassword: String?, token: String): User {
        val body = JSONObject()
            .put("name", user.name)
            .put("email", user.email)
            .put("role", user.role)
            .put("status", user.status)
        if (!newPassword.isNullOrBlank()) body.put("password", newPassword)
        return User.fromJson(JSONObject(request("PUT", "/api/users/${encode(user.id)}", body, token)))
    }

    suspend fun deleteUser(id: String, token: String) {
        request("DELETE", "/api/users/${encode(id)}", body = null, token = token)
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    private suspend fun request(
        method: String,
        path: String,
        body: JSONObject?,
        token: String?
    ): String = withContext(Dispatchers.IO) {
        val connection = (URL(BASE_URL + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            if (token != null) setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        try {
            if (body != null) {
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val message = runCatching { JSONObject(text).optString("message") }
                    .getOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?: "Error $code"
                throw ApiException(code, message)
            }
            text
        } catch (e: IOException) {
            throw ApiException(0, "No se pudo contactar al servidor. Revisa tu conexión a internet.")
        } finally {
            connection.disconnect()
        }
    }
}

class ApiException(val status: Int, message: String) : Exception(message)
