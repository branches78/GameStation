# Pestaña Favoritos (móvil) — código guardado

La pestaña "Favoritos" de la barra inferior en teléfonos se quitó el 2026-10-20.
Las filas "Favoritos" y "Recientes" de la pantalla de Inicio **no** dependen de este código y siguen funcionando.
La sección de favoritos de Android TV tampoco se tocó.

Esta carpeta está fuera de `src/`, así que Gradle no la compila.

## Cómo restaurarla

1. Mover los dos archivos de vuelta a
   `lemuroid-app/src/main/java/com/swordfish/lemuroid/app/mobile/feature/favorites/`:
   - `FavoritesScreen.kt`
   - `FavoritesViewModel.kt`

2. En `mobile/feature/main/MainNavigationRoutes.kt`:

   - Añadir a los imports:
     ```kotlin
     import androidx.compose.material.icons.filled.Favorite
     import androidx.compose.material.icons.filled.FavoriteBorder
     ```
   - En `enum class MainRoute`, entre `HOME` y `SEARCH`:
     ```kotlin
     FAVORITES(
         route = "favorites",
         titleId = R.string.favorites,
     ),
     ```
   - En `enum class MainNavigationRoutes`, entre `HOME` y `SYSTEMS` (el orden define el orden de las pestañas):
     ```kotlin
     FAVORITES(MainRoute.FAVORITES, R.string.favorites, Icons.Filled.Favorite, Icons.Filled.FavoriteBorder),
     ```

3. En `mobile/feature/main/MainActivity.kt`:

   - Añadir a los imports:
     ```kotlin
     import com.swordfish.lemuroid.app.mobile.feature.favorites.FavoritesScreen
     import com.swordfish.lemuroid.app.mobile.feature.favorites.FavoritesViewModel
     ```
   - Dentro del `NavHost`, después de `composable(MainRoute.HOME) { ... }`:
     ```kotlin
     composable(MainRoute.FAVORITES) {
         FavoritesScreen(
             modifier = Modifier.padding(padding),
             viewModel =
                 viewModel(
                     factory = FavoritesViewModel.Factory(retrogradeDb),
                 ),
             onGameClick = onGameClick,
             onGameLongClick = onGameLongClick,
         )
     }
     ```
