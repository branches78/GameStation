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
6. **Commits:** no hagas commit ni push de nada hasta que el usuario lo diga, aunque un hook lo pida.
7. **Textos en todos los idiomas:** cualquier cambio de texto visible se aplica al español, al inglés por
   defecto (`values/`) y a **todos** los idiomas de `res/values-*/strings.xml`, con traducción propia.
   Si a un idioma le falta la cadena, se añade.

## Trabajo en la nube vs. local

- **En la nube** (claude.ai/code): solo se hacen **correcciones de código**, commit y push.
  **No compiles** ni intentes instalar el APK: la nube no tiene el SDK de Android configurado
  ni acceso a los dispositivos del usuario.
- **En local** (PC del usuario, Windows): se compila, se copia el APK y se instala en la TV.
  - Repositorio: `D:\Descargas\Proyectos\PlayRetro\Source-Code` (remoto `https://github.com/branches78/PlayRetro.git`)
  - APKs: copiar a `D:\Descargas\Proyectos\PlayRetro\APK-Releases\` como `PlayRetro-debug.apk`
    (sale de `lemuroid-app\build\outputs\apk\freeDynamic\debug\`). En cmd, cambiar de unidad con `cd /d`.
  - Compilar con `JAVA_HOME=C:\Users\Ricardo\.jdks\jbr-21.0.11` (el JDK de Android Studio es Java 25, demasiado nuevo
    para Gradle 8.10.2): `./gradlew --no-daemon :lemuroid-app:assembleFreeDynamicDebug`
  - SDK en `D:\Aplicaciones\AndroidSDK` (`local.properties`, no se sube al repositorio).
  - Tras cada compilación autorizada, instalar **solo en la TV** (BuzzTV, `192.168.18.56:5555`) con
    `adb install -r`, **sin abrir la app**. En el teléfono la instala el usuario.

## Datos del proyecto

- **Nombre visible:** **PlayRetro** ("GameStation" ya existía en Play Store y "RetroDeck" es un proyecto de PC conocido). Se define en
  `lemuroid-app/build.gradle.kts` (`lemuroid_name`), en los textos de `strings.xml` y en el banner de TV
  (`res/mipmap-xhdpi/ic_banner.png`). El repositorio, las carpetas y los nombres internos siguen como GameStation.
- `applicationId`: `com.playretro.tv` (debug: `com.playretro.tv.debug`), versión `1.0.0` (`versionCode` 1).
  Antes era `com.gamestation.tv`: es una app distinta para Android, así que la antigua hay que desinstalarla aparte
  y sus datos (partidas, ajustes, biblioteca) no pasan a la nueva.
- Variante que se compila: **freeDynamicDebug** (sin librerías de Google; los cores se descargan al usarlos).
  La sincronización con Google Drive solo existe en la variante `play` y requiere credenciales propias en Google Cloud.
- Tema: verde suave fijo en teléfono y TV (sin Material You, sin el verde fluorescente): verde claro #D4E8D0 para textos e iconos (`primary`) y verde oscuro #3A4B39 para rellenos (`primaryContainer`, `secondaryContainer`, interruptores de Ajustes). Colores en
  `lemuroid-app/src/main/java/.../mobile/shared/compose/ui/Color.kt` y `res/values/material-colors.xml` (deben coincidir).
- Base de datos de la app (Room): versión 11 (`fileSize` y `fileLastModified` para detectar archivos cambiados al re-escanear).

## Piezas importantes

- **Inicio (filas):** `app/shared/home/HomeDynamicRows.kt` (filas dinámicas compartidas por móvil y TV),
  `mobile/feature/home/HomeScreen.kt`, `tv/home/TVHomeFragment.kt`.
  Orden: Continuar jugando, Favoritos, Añadidos recientemente, Porque jugaste, Vuelve a jugar, Descubrir,
  Clásicos de…, Más de…. Las filas sin juegos se ocultan (Añadidos recientemente siempre tiene juegos);
  con la biblioteca vacía no hay filas.
- **TV:** pantalla de inicio con pestañas (Inicio, Consolas, Ajustes, Acerca de) en `tv/main/TVMainTabsFragment.kt`,
  todas con el mismo ancho de contenido centrado (`tv_content_width`, 672 dp). Las filas de Inicio avanzan de una tarjeta
  en una con la enfocada en la primera posición y se recortan en ese ancho (`HomeListRowPresenter` en `TVHomeFragment.kt`).
  Los juegos de una consola (`tv/games/TVGamesFragment.kt`) y "Mostrar todo" de Favoritos (`tv/favorites/TVFavoritesFragment.kt`)
  se abren dentro de su pestaña, sin ocultar la barra (`showSystemGames` / `showFavorites`; Atrás vuelve a la pestaña),
  con las mismas tarjetas cuadradas de Inicio, 4 por fila y alineadas a la izquierda (`TVTabGrids.setUpHomeCardsGrid`).
  "Configurar núcleo" del menú de un juego muestra las opciones dentro de la misma ventana (`TVGameContextMenu.kt`).
  El título de esas rejillas va alineado a la izquierda sobre una franja opaca, y las carátulas pasan por debajo al
  hacer scroll (`TVTabGrids.addTitle`). La pestaña se llama "Consolas" también en el móvil (`title_consoles`).
  Las rejillas de TV no oscurecen las tarjetas sin foco (`VerticalGridPresenter(..., useFocusDimmer = false)`); Ajustes de TV reutilizan las
  pantallas Compose del móvil (`tv/settings/TVSettingsTabFragment.kt`); estilo de tarjetas en `tv/shared/TVCardStyle.kt`.
- **Carátulas:** forma por sistema en `app/shared/covers/CoverAspectRatio.kt`, sustituida por la forma real de cada
  carátula una vez cargada (`CoverRatioCache.kt`). En las rejillas la tarjeta mantiene la del sistema
  (`matchCoverShape = false`); en las filas de Inicio del móvil todas son verticales, del ancho de una casilla de la rejilla,
  y en la TV (filas de Inicio, juegos de una consola y Favoritos) todas son cuadradas, 4 por fila (`fixedRatio`); las carátulas de otra forma se muestran enteras y
  centradas dentro. Alternativas de nombre y caché de
  resultados en `ThumbnailFallbackInterceptor.kt`. Reconocimiento de juegos (CRC, TOSEC → No-Intro, búsqueda por título)
  en `lemuroid-metadata-libretro-db/.../LibretroDBMetadataProvider.kt`.
- **Iconos de sistemas:** PNG del tema "FlatUI" de RetroArch (`retroarch-assets`, CC BY 4.0, crédito en `README.md`)
  en `retrograde-app-shared/src/main/res/drawable-nodpi/game_system_*.png`, sobre fondo oscuro de tarjeta.
- **Opciones de cores:** Ajustes → Otros → Cores lista todos los sistemas (`CoresSelectionScreen.kt`) y cada uno abre
  `CoreOptionsSettingsScreen.kt` (ruta `settings/cores/{systemId}`, también en la TV). Las opciones y sus valores los
  envía el core al jugar y se guardan en `lib/core/CoreOptionsCache.kt` (Harmony, el juego corre en el proceso `:game`).
  Las filas son las mismas que en el menú del juego (`shared/coreoptions/CoreOptionsItems.kt`) y comparten los valores.
  Valores por defecto de la app en `GameSystem.kt` (`defaultSettings`): Dynarec de PlayStation y JIT de DS activados.
  **Por juego:** "Configurar núcleo" de un juego y el menú dentro del juego tienen las pestañas "Solo este juego"
  (a la izquierda y elegida por defecto) y "Todos los juegos de [consola]" (`CoreOptionsScope`,
  `CoreOptionsScopeSelector.kt`). Los valores de un juego se guardan como `cvg_<gameId>_<opción>` y el core propio como
  `pref_key_game_core_selection_<gameId>`; al jugar mandan sobre los de la consola (`CoreVariablesManager`,
  `CoresSelection.getCoreConfigForGame`). Las opciones cambiadas llevan la marca "Este juego".
  **Relación de aspecto** (grupo "Pantalla"): Nintendo 64 (`mupen64plus-aspect`: 4:3, 16:9 estirado, 16:9 adjusted =
  pantalla ancha) y SwanStation (`swanstation_Display_AspectRatio` + widescreen hack). PCSX ReARMed no la tiene:
  se indica que hay que elegir SwanStation para ese juego. LibretroDroid no permite forzar la proporción desde la app.
- **Cores (descarga):** la app los baja de `github.com/branches78/PlayRetroCores` (fork de LemuroidCores), etiqueta
  `pr-1.0.0`, ruta `lemuroid_core_<core>/src/main/jniLibs/<abi>/lib<core>_libretro_android.so`
  (`baseUri` en `lemuroid-app-ext-free/.../CoreUpdaterImpl.kt`; `CORES_VERSION` y las huellas de cada core en
  `CoreChecksums.kt`, junto a él). Para añadir o actualizar un core: subirlo a `master` de PlayRetroCores, crear una
  etiqueta nueva, cambiar `CORES_VERSION` y regenerar las huellas (si no coinciden, la app rechaza el core).
  PlayStation tiene dos cores: PCSX ReARMed (por defecto) y SwanStation (necesita BIOS scph5500/5501/5502).
  Los archivos extra de PSP (`assets/ppsspp.zip`, versión 1.15) también se bajan de PlayRetroCores, por el commit
  de esa versión (`PPSSPPAssetsManager.kt`). Las descargas de cores se hacen a un `.part` y se renombran al terminar.
- **Progreso del escaneo:** `LemuroidLibrary.indexLibrary` lista primero todos los archivos y después los procesa,
  avisando del progreso (`IndexProgress`); después `LibraryIndexWork` descarga los cores de los sistemas encontrados
  (antes lo hacía `CoreUpdateWork` aparte). Publica las dos etapas con `setProgress` (`LibraryLoadingProgress`:
  archivos hasta el 85 %, cores el resto) y `PendingOperationsMonitor.directoryScanProgress()` lo lee. Con la biblioteca vacía, Inicio (móvil y TV) y Consolas (TV)
  muestran "Cargando tus juegos" con barra y porcentaje en lugar del cuadro de la carpeta (`TVEmptyLibraryCard.kt`,
  `HomeLibraryLoading` en `HomeScreen.kt`); al re-escanear, la barra sale bajo "Directorio" en Ajustes
  (`LibraryScanProgress.kt`). Ya no hay línea de progreso en el móvil ni círculo de carga en la TV.
- **Acerca de:** misma información en la pestaña "Acerca de" de la TV (`tv/about/TVAboutFragment.kt`) y en la ventana
  del botón "?" del móvil: `shared/about/AboutContent.kt` (qué es PlayRetro, ayuda y licencia/créditos).
- **Favoritos (móvil):** ya no es pestaña. Con más de 10 favoritos, la fila de Inicio termina en una tarjeta
  "Mostrar todo" que abre `mobile/feature/favorites/FavoritesScreen.kt` (ruta `MainRoute.FAVORITES`), igual que en la TV.
  Cómo volver a hacerla pestaña: `saved-code/favorites-tab/RESTORE.md`.

## Revisiones

- Ronda 1 (2026-10-21): corregidos todos los hallazgos (código muerto de Ajustes y asignación de mandos de TV,
  textos sin uso, descargas de cores atómicas y verificadas con `CoreChecksums.kt`, Zip Slip en ROM y en archivos
  de PSP, enlaces externos inválidos, BIOS de SwanStation, carátulas por HTTPS, firma fuera del código en
  `keystore.properties`). Al cambiar `CORES_VERSION` hay que regenerar `CoreChecksums.kt` con `git ls-tree -r <tag>`.

## Pendientes conocidos

- Correcciones de navegación con el mando en TV (el usuario las irá indicando).
- Sincronización con Google Drive (variante `play` + configuración en Google Cloud).
- Actualización automática de cores en `PlayRetroCores`: acción de GitHub que descargue periódicamente del buildbot
  de libretro los cores que usa la app y los suba a una rama aparte (`updates`) para probarlos antes de pasarlos
  a `master` y crear una etiqueta nueva. Acordado implementarlo más adelante.
