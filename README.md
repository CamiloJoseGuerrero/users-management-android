# Gestión de Usuarios — App Android

App en **Kotlin + Jetpack Compose** que consume la API REST del proyecto
`users-management-spring-boot-hexagonal-ddd`, desplegada en Render:
`https://users-management-camilo.onrender.com`.

## Funciones

| Pantalla / acción | Endpoint | Quién puede |
|---|---|---|
| Iniciar sesión | `POST /api/auth/login` | Cualquiera |
| Crear cuenta (registro) | `POST /api/users` | Cualquiera (queda PENDING) |
| Ver lista de usuarios | `GET /api/users` | ADMIN, REVIEWER |
| Nuevo usuario | `POST /api/users` | ADMIN |
| Editar / activar | `PUT /api/users/{id}` | ADMIN |
| Eliminar | `DELETE /api/users/{id}` | ADMIN |

- El token JWT dura 15 minutos; la barra superior muestra la cuenta regresiva y cierra la sesión al expirar.
- Si Render está dormido, aparece un aviso mientras despierta (hasta ~90 s).

## Estructura

```
app/src/main/java/com/camilo/usersmanagement/
├── MainActivity.kt        # Activity + tema Material 3
├── data/ApiClient.kt      # Llamadas HTTP (HttpURLConnection + org.json)
├── data/Models.kt         # User, Session (decodifica el JWT)
└── ui/MainViewModel.kt    # Estado y lógica
    ui/Screens.kt          # Pantallas Compose
```

## Probar contra el backend local

En `ApiClient.kt` cambia `BASE_URL` por `http://10.0.2.2:8080` (el emulador ve tu PC en esa IP)
y arranca el backend con `.\mvnw.cmd spring-boot:run`.
