# Pestaña Favoritos (móvil) — cómo volver a convertirla en pestaña

La pestaña "Favoritos" de la barra inferior en teléfonos se quitó el 2026-10-20.

Desde el 2026-10-21 su pantalla vuelve a estar en la app, pero **no como pestaña**: se abre desde la tarjeta
"Mostrar todo" al final de la fila Favoritos de Inicio (solo aparece con más de 10 favoritos).
El código está en `lemuroid-app/src/main/java/com/swordfish/lemuroid/app/mobile/feature/favorites/`
y la ruta es `MainRoute.FAVORITES` (con `parent = HOME`).

## Cómo volver a tenerla como pestaña

1. En `mobile/feature/main/MainNavigationRoutes.kt`:

   - Añadir a los imports:
     ```kotlin
     import androidx.compose.material.icons.filled.Favorite
     import androidx.compose.material.icons.filled.FavoriteBorder
     ```
   - En `MainRoute.FAVORITES`, quitar `parent = HOME` (así se muestra como pantalla principal, sin flecha de volver).
   - En `enum class MainNavigationRoutes`, entre `HOME` y `SYSTEMS` (el orden define el orden de las pestañas):
     ```kotlin
     FAVORITES(MainRoute.FAVORITES, R.string.favorites, Icons.Filled.Favorite, Icons.Filled.FavoriteBorder),
     ```

2. Opcional: quitar la tarjeta "Mostrar todo" de la fila Favoritos en `mobile/feature/home/HomeScreen.kt`
   (parámetro `onShowAll`), ya que la pestaña cumpliría la misma función.
