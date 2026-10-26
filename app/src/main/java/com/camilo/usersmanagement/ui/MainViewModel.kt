package com.camilo.usersmanagement.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.camilo.usersmanagement.data.ApiClient
import com.camilo.usersmanagement.data.ApiException
import com.camilo.usersmanagement.data.Session
import com.camilo.usersmanagement.data.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class Screen { LOGIN, REGISTER, USERS }

data class UiMessage(val text: String, val isError: Boolean, val id: Long = System.nanoTime())

class MainViewModel : ViewModel() {

    var screen by mutableStateOf(Screen.LOGIN)
        private set
    var session by mutableStateOf<Session?>(null)
        private set
    var users by mutableStateOf<List<User>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    /** true cuando una peticion tarda: Render esta despertando. */
    var wakingServer by mutableStateOf(false)
        private set
    var message by mutableStateOf<UiMessage?>(null)

    fun goTo(target: Screen) {
        screen = target
    }

    fun login(email: String, password: String) = launchApi {
        val token = ApiClient.login(email.trim(), password)
        val newSession = Session.fromToken(token)
            ?: throw ApiException(0, "El servidor devolvió un token no válido.")
        session = newSession
        screen = Screen.USERS
        info("Sesión iniciada como ${newSession.role}.")
        loadUsersInternal()
    }

    fun register(user: User, password: String, onDone: () -> Unit) = launchApi {
        ApiClient.createUser(user, password, token = null)
        info("Cuenta creada. Revisa tu correo; un administrador debe activarla.")
        screen = Screen.LOGIN
        onDone()
    }

    fun loadUsers() = launchApi { loadUsersInternal() }

    fun saveUser(original: User?, edited: User, password: String, onDone: () -> Unit) = launchApi {
        val token = requireToken()
        if (original == null) {
            ApiClient.createUser(edited, password, token)
            info("Usuario creado (PENDING). Se envió el correo de bienvenida.")
        } else {
            ApiClient.updateUser(edited, password.ifBlank { null }, token)
            info("Usuario actualizado. Se envió un correo de notificación.")
        }
        onDone()
        loadUsersInternal()
    }

    fun activate(user: User) = launchApi {
        ApiClient.updateUser(user.copy(status = "ACTIVE"), null, requireToken())
        info("${user.name} ahora está ACTIVE.")
        loadUsersInternal()
    }

    fun delete(user: User) = launchApi {
        ApiClient.deleteUser(user.id, requireToken())
        info("Usuario eliminado.")
        loadUsersInternal()
    }

    fun logout(reason: String? = null) {
        session = null
        users = emptyList()
        screen = Screen.LOGIN
        if (reason != null) showError(reason) else info("Sesión cerrada.")
    }

    // ----------------------------------------------------------------------

    private suspend fun loadUsersInternal() {
        val current = session ?: return
        if (!current.canList) {
            users = emptyList()
            showError("Tu rol (${current.role}) no puede ver la lista. Solo ADMIN y REVIEWER.")
            return
        }
        users = ApiClient.getUsers(current.token)
    }

    private fun requireToken(): String =
        session?.token ?: throw ApiException(401, "Inicia sesión primero.")

    private fun info(text: String) { message = UiMessage(text, isError = false) }
    private fun showError(text: String) { message = UiMessage(text, isError = true) }

    /** Ejecuta una llamada a la API mostrando carga y traduciendo errores. */
    private fun launchApi(block: suspend () -> Unit) {
        if (loading) return
        viewModelScope.launch {
            loading = true
            val slowTimer = launch {
                delay(4_000)
                wakingServer = true
            }
            try {
                block()
            } catch (e: ApiException) {
                when {
                    e.status == 401 && session != null ->
                        logout("Tu sesión expiró o el token no es válido. Inicia sesión de nuevo.")
                    e.status == 403 -> showError("No tienes permiso para esta acción.")
                    else -> showError(e.message ?: "Error inesperado.")
                }
            } catch (e: Exception) {
                showError(e.message ?: "Error inesperado.")
            } finally {
                slowTimer.cancel()
                wakingServer = false
                loading = false
            }
        }
    }
}
