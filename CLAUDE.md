# GameStation — instrucciones para Claude

GameStation es un fork independiente de [Lemuroid](https://github.com/Swordfish90/Lemuroid) (GPL-3.0),
un emulador para Android con interfaz para teléfono (Jetpack Compose) y para Android TV (Leanback).
No se sincroniza con Lemuroid: el código es propio y va por su cuenta.

## Reglas de trabajo (obligatorias)

1. **Idioma:** responde siempre en **español**, incluidos los mensajes cortos de progreso y los resúmenes.
   Los **comentarios dentro del código** van en **inglés**, como el resto del código.
2. **Autorización:** no corrijas, edites ni compiles nada hasta que el usuario lo autorice explícitamente.
   Investigar y leer código sí está permitido. Cada cambio o compilación necesita su propia autorización.
3. **Una corrección a la vez:** el usuario menciona las correcciones una por una; aplica solo la que pide.
4. **Revisiones de código muerto, bugs o seguridad:** empieza siempre por un **resumen de hallazgos**
   (cantidad por tipo y gravedad) y después el detalle. No corrijas nada hasta que el usuario lo apruebe.
5. **Diseño:** antes de cambios visuales grandes, ofrece un mockup.

## Trabajo en la nube vs. local

- **En la nube** (claude.ai/code): solo se hacen **correcciones de código**, commit y push.
  **No compiles** ni intentes instalar el APK: la nube no tiene el SDK de Android configurado
  ni acceso a los dispositivos del usuario.
- **En local** (PC del usuario, Windows): se compila, se copia el APK y se instala en la TV.
  - Repositorio: `D:\Descargas\Proyectos\GameStation\GameStation-App`
  - APKs: copiar a `D:\Descargas\Proyectos\GameStation\APK Releases\` como `GameStation-<versión>-free-dynamic-debug.apk`
  - Compilar con `JAVA_HOME=C:\Users\Ricardo\.jdks\jbr-21.0.11` (el JDK de Android Studio es Java 25, demasiado nuevo
    para Gradle 8.10.2): `./gradlew --no-daemon :lemuroid-app:assembleFreeDynamicDebug`
  - SDK en `D:\Aplicaciones\AndroidSDK` (`local.properties`, no se sube al repositorio).
  - Tras cada compilación autorizada, instalar **solo en la TV** (BuzzTV, `192.168.18.56:5555`) con
    `adb install -r`, **sin abrir la app**. En el teléfono la instala el usuario.

## Datos del proyecto

- `applicationId`: `com.gamestation.tv` (debug: `com.gamestation.tv.debug`), versión `1.0.0` (`versionCode` 1).
- Variante que se compila: **freeDynamicDebug** (sin librerías de Google; los cores se descargan al usarlos).
  La sincronización con Google Drive solo existe en la variante `play` y requiere credenciales propias en Google Cloud.
- Tema: verde GameStation fijo en teléfono y TV (sin Material You). Colores en
  `lemuroid-app/src/main/java/.../mobile/shared/compose/ui/Color.kt` y `res/values/material-colors.xml` (deben coincidir).
- Base de datos de la app (Room): versión 11 (`fileSize` y `fileLastModified` para detectar archivos cambiados al re-escanear).

## Piezas importantes

- **Inicio (filas):** `app/shared/home/HomeDynamicRows.kt` (filas dinámicas compartidas por móvil y TV),
  `mobile/feature/home/HomeScreen.kt`, `tv/home/TVHomeFragment.kt`.
  Orden: Recientes, Favoritos, filas dinámicas, Descubrir, más filas dinámicas.
  Recientes y Favoritos se ocultan si están vacías; Descubrir se oculta con la biblioteca vacía.
- **TV:** pantalla de inicio con pestañas en `tv/main/TVMainTabsFragment.kt`; Ajustes de TV reutilizan las
  pantallas Compose del móvil (`tv/settings/TVSettingsTabFragment.kt`); estilo de tarjetas en `tv/shared/TVCardStyle.kt`.
- **Carátulas:** forma por sistema en `app/shared/covers/CoverAspectRatio.kt`; alternativas de nombre y caché de
  resultados en `ThumbnailFallbackInterceptor.kt`. Reconocimiento de juegos (CRC, TOSEC → No-Intro, búsqueda por título)
  en `lemuroid-metadata-libretro-db/.../LibretroDBMetadataProvider.kt`.
- **Código guardado:** la pestaña Favoritos del móvil se quitó; su código está en `saved-code/favorites-tab/` con instrucciones.

## Pendientes conocidos

- Correcciones de navegación con el mando en TV (el usuario las irá indicando).
- Código muerto: `TVSettingsActivity`, `TVSettingsFragment`, `res/xml/tv_settings.xml` y ayudantes de preferencias de TV
  ya no se abren desde ningún sitio.
- Sincronización con Google Drive (variante `play` + configuración en Google Cloud).
