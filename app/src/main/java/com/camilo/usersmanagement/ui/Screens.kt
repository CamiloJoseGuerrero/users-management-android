package com.camilo.usersmanagement.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.camilo.usersmanagement.data.ROLES
import com.camilo.usersmanagement.data.STATUSES
import com.camilo.usersmanagement.data.User
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// Raiz de la app
// ---------------------------------------------------------------------------

@Composable
fun UsersApp(vm: MainViewModel) {
    val snackbar = remember { SnackbarHostState() }
    val msg = vm.message
    LaunchedEffect(msg?.id) {
        if (msg != null) {
            snackbar.showSnackbar(msg.text)
            vm.message = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { if (vm.screen == Screen.USERS) UsersTopBar(vm) },
        floatingActionButton = {
            if (vm.screen == Screen.USERS && vm.session?.isAdmin == true) {
                var showNew by remember { mutableStateOf(false) }
                FloatingActionButton(onClick = { showNew = true }) { Text("+", fontSize = 26.sp) }
                if (showNew) UserFormDialog(vm, original = null, onDismiss = { showNew = false })
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (vm.wakingServer) WakingBanner()
            when (vm.screen) {
                Screen.LOGIN -> LoginScreen(vm)
                Screen.REGISTER -> RegisterScreen(vm)
                Screen.USERS -> UsersScreen(vm)
            }
        }
    }
}

@Composable
private fun WakingBanner() {
    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(
                "Despertando el servidor de Render… en el plan gratuito la primera petición puede tardar hasta 1 minuto y medio.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}

// ---------------------------------------------------------------------------
// Login y registro
// ---------------------------------------------------------------------------

@Composable
private fun AuthCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        Text("Gestión de Usuarios", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Card(Modifier.widthIn(max = 460.dp).fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                content()
            }
        }
    }
}

@Composable
private fun LoginScreen(vm: MainViewModel) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    AuthCard("Iniciar sesión", "Ingresa con tu usuario para administrar las cuentas.") {
        OutlinedTextField(
            value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        PasswordField(password, { password = it }, "Contraseña")
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { vm.login(email, password) },
            enabled = !vm.loading && email.isNotBlank() && password.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) { ButtonContent(vm.loading, "Entrar") }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { vm.goTo(Screen.REGISTER) }, modifier = Modifier.fillMaxWidth()) {
            Text("¿No tienes cuenta? Crear una cuenta")
        }
    }
}

@Composable
private fun RegisterScreen(vm: MainViewModel) {
    BackHandler { vm.goTo(Screen.LOGIN) }
    var id by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    AuthCard(
        "Crear cuenta",
        "La cuenta queda PENDING hasta que un administrador la active. Recibirás un correo de bienvenida."
    ) {
        OutlinedTextField(id, { id = it }, label = { Text("ID (ej. 0010)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            email, { email = it }, label = { Text("Email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        PasswordField(password, { password = it }, "Contraseña (mínimo 8 caracteres)")
        Spacer(Modifier.height(16.dp))
        val valid = id.isNotBlank() && name.trim().length >= 3 && email.contains("@") && password.length >= 8
        Button(
            onClick = {
                vm.register(User(id.trim(), name.trim(), email.trim(), "MEMBER", "PENDING"), password) {
                    id = ""; name = ""; email = ""; password = ""
                }
            },
            enabled = !vm.loading && valid,
            modifier = Modifier.fillMaxWidth()
        ) { ButtonContent(vm.loading, "Crear cuenta") }
        TextButton(onClick = { vm.goTo(Screen.LOGIN) }, modifier = Modifier.fillMaxWidth()) {
            Text("← Volver al inicio de sesión")
        }
    }
}

// ---------------------------------------------------------------------------
// Lista de usuarios
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UsersTopBar(vm: MainViewModel) {
    val session = vm.session ?: return
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(session.token) {
        while (true) {
            now = System.currentTimeMillis()
            if (session.expiresAtMs in 1..now) {
                vm.logout("El token expiró (dura 15 minutos). Inicia sesión de nuevo.")
                break
            }
            delay(1_000)
        }
    }
    val remaining = ((session.expiresAtMs - now) / 1000).coerceAtLeast(0)
    TopAppBar(
        title = {
            Column {
                Text("Usuarios", fontWeight = FontWeight.Bold)
                Text(
                    "${session.role} · expira en %02d:%02d".format(remaining / 60, remaining % 60),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        actions = {
            TextButton(onClick = { vm.loadUsers() }, enabled = !vm.loading) { Text("Recargar") }
            TextButton(onClick = { vm.logout() }) { Text("Salir") }
        }
    )
}

@Composable
private fun UsersScreen(vm: MainViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf<User?>(null) }
    var deleting by remember { mutableStateOf<User?>(null) }
    val isAdmin = vm.session?.isAdmin == true

    val filtered = vm.users.filter { u ->
        query.isBlank() || listOf(u.id, u.name, u.email).any { it.contains(query.trim(), ignoreCase = true) }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item { Stats(vm.users) }
        item {
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                label = { Text("Buscar por nombre, email o ID") }, modifier = Modifier.fillMaxWidth()
            )
        }
        if (vm.loading && vm.users.isEmpty()) {
            item {
                Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (filtered.isEmpty()) {
            item {
                Text(
                    "No hay usuarios para mostrar.",
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(filtered, key = { it.id }) { user ->
            UserCard(
                user = user,
                isAdmin = isAdmin,
                busy = vm.loading,
                onActivate = { vm.activate(user) },
                onEdit = { editing = user },
                onDelete = { deleting = user }
            )
        }
    }

    editing?.let { user -> UserFormDialog(vm, original = user, onDismiss = { editing = null }) }

    deleting?.let { user ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Eliminar usuario") },
            text = { Text("¿Eliminar a ${user.name} (${user.email})? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = { vm.delete(user); deleting = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun Stats(users: List<User>) {
    fun count(s: String) = users.count { it.status == s }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard("Total", users.size)
        StatCard("Activos", count("ACTIVE"))
        StatCard("Pendientes", count("PENDING"))
        StatCard("Bloq./inactivos", count("BLOCKED") + count("INACTIVE"))
    }
}

@Composable
private fun StatCard(label: String, value: Int) {
    Card {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text("$value", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun UserCard(
    user: User,
    isAdmin: Boolean,
    busy: Boolean,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(user.email, style = MaterialTheme.typography.bodyMedium)
            Text(
                "ID: ${user.id}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Badge(user.role, roleColor(user.role))
                Badge(user.status, statusColor(user.status))
            }
            if (isAdmin) {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (user.status != "ACTIVE") {
                        Button(onClick = onActivate, enabled = !busy) { Text("Activar") }
                    }
                    OutlinedButton(onClick = onEdit, enabled = !busy) { Text("Editar") }
                    OutlinedButton(
                        onClick = onDelete, enabled = !busy,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Eliminar") }
                }
            }
        }
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, color),
        color = Color.Transparent
    ) {
        Text(
            text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun roleColor(role: String): Color = when (role) {
    "ADMIN" -> MaterialTheme.colorScheme.primary
    "REVIEWER" -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun statusColor(status: String): Color = when (status) {
    "ACTIVE" -> Color(0xFF1F8A4C)
    "PENDING" -> Color(0xFFB26B00)
    "BLOCKED" -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

// ---------------------------------------------------------------------------
// Formulario crear / editar
// ---------------------------------------------------------------------------

@Composable
private fun UserFormDialog(vm: MainViewModel, original: User?, onDismiss: () -> Unit) {
    val editing = original != null
    var id by rememberSaveable { mutableStateOf(original?.id.orEmpty()) }
    var name by rememberSaveable { mutableStateOf(original?.name.orEmpty()) }
    var email by rememberSaveable { mutableStateOf(original?.email.orEmpty()) }
    var password by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf(original?.role ?: "MEMBER") }
    var status by rememberSaveable { mutableStateOf(original?.status ?: "ACTIVE") }

    val passwordOk = if (editing) password.isEmpty() || password.length >= 8 else password.length >= 8
    val valid = id.isNotBlank() && name.trim().length >= 3 && email.contains("@") && passwordOk

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing) "Editar usuario" else "Nuevo usuario") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    id, { id = it }, label = { Text("ID") }, singleLine = true,
                    enabled = !editing, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    email, { email = it }, label = { Text("Email") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                PasswordField(password, { password = it }, if (editing) "Nueva contraseña (opcional)" else "Contraseña")
                Text(
                    if (editing) "Déjala vacía para conservar la actual." else "Mínimo 8 caracteres. Queda PENDING y recibe un correo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Text("Rol", style = MaterialTheme.typography.labelLarge)
                OptionChips(ROLES, role) { role = it }
                if (editing) {
                    Spacer(Modifier.height(6.dp))
                    Text("Estado", style = MaterialTheme.typography.labelLarge)
                    OptionChips(STATUSES, status) { status = it }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = valid && !vm.loading,
                onClick = {
                    val user = User(id.trim(), name.trim(), email.trim(), role, status)
                    vm.saveUser(original, user, password, onDone = onDismiss)
                }
            ) { ButtonContent(vm.loading, "Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionChips(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(option) }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Componentes comunes
// ---------------------------------------------------------------------------

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ButtonContent(loading: Boolean, label: String) {
    if (loading) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary
        )
        Spacer(Modifier.size(8.dp))
        Text("Procesando…")
    } else {
        Text(label)
    }
}
