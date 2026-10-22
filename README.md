# PlayRetro

PlayRetro is a retro game emulator for Android phones, tablets and Android TV, based on
[libretro](https://www.libretro.com/) cores.

It started as a fork of [Lemuroid](https://github.com/Swordfish90/Lemuroid) by Filippo Scognamiglio (Swordfish90)
and is now developed independently. Like the original project, it is distributed under the
GNU General Public License v3.0 (see `COPYING`).

## Supported systems

| System | Core |
|---|---|
| Atari 2600 | [Stella](https://docs.libretro.com/library/stella/) |
| Atari 7800 | [ProSystem](https://docs.libretro.com/library/prosystem/) |
| Atari Lynx | [Handy](https://docs.libretro.com/library/handy/) |
| Nintendo (NES) | [FCEUmm](https://docs.libretro.com/library/fceumm/) |
| Super Nintendo (SNES) | [Snes9x](https://docs.libretro.com/library/snes9x/) |
| Nintendo 64 | [Mupen64Plus-Next](https://docs.libretro.com/library/mupen64plus/) |
| Game Boy / Game Boy Color | [Gambatte](https://docs.libretro.com/library/gambatte/) |
| Game Boy Advance | [mGBA](https://docs.libretro.com/library/mgba/) |
| Nintendo DS | [melonDS](https://docs.libretro.com/library/melonds/) |
| Nintendo 3DS (beta, 64-bit only) | [Citra](https://docs.libretro.com/library/citra/) |
| Sega Genesis / Mega Drive, Sega CD, Master System, Game Gear | [Genesis Plus GX](https://docs.libretro.com/library/genesis_plus_gx/) |
| PlayStation | [PCSX ReARMed](https://docs.libretro.com/library/pcsx_rearmed/) (default) or [SwanStation](https://github.com/libretro/swanstation) |
| PlayStation Portable | [PPSSPP](https://docs.libretro.com/library/ppsspp/) |
| Arcade | [FinalBurn Neo](https://github.com/libretro/FBNeo/), [MAME 2003-Plus](https://docs.libretro.com/library/mame2003_plus/) |
| NEC PC Engine | [Beetle PCE Fast](https://docs.libretro.com/library/beetle_pce_fast/) |
| Neo Geo Pocket / Pocket Color | [Beetle NeoPop](https://docs.libretro.com/library/beetle_neopop/) |
| WonderSwan / WonderSwan Color | [Beetle Cygne](https://docs.libretro.com/library/beetle_cygne/) |
| DOS (beta) | [DOSBox Pure](https://docs.libretro.com/library/dosbox_pure/) |

Cores are downloaded the first time a system is used and verified before they run.
SwanStation needs a real PlayStation BIOS (`scph5500.bin`, `scph5501.bin` or `scph5502.bin`, depending on the game region).

## Features

**Library and home screen**
- Automatic ROM scanning and indexing, including zipped ROMs, with game recognition by checksum and name.
- Box art from the libretro thumbnails server, shown with the real shape of each cover.
- Home rows: Continue playing, Favorites, Recently added, Because you played…, Play again, Discover,
  Classics of a different system every day and More from the same developer.
- Systems screen with colored system icons, and grid or list view for the games of each system.

**Android TV**
- Top bar with Home, Systems and Settings tabs, search and full remote control navigation.
- The same settings screens as on phones.

**Settings**
- Organized in ROMs, Display, Sound, Input and Others.
- Cores screen: choose the core of each system (when there is more than one) and change its options
  outside of the game. The same options are available from the in-game menu.
- Sensible defaults for slow devices: PlayStation dynamic recompiler and Nintendo DS JIT enabled.

**Gameplay**
- Automatic save and restore of game states, quick save and load.
- Optimized and customizable touch controls (size and position), tilt input.
- Gamepad support with button remapping and local multiplayer.
- Display filters (LCD/CRT) and HD mode.
- Cloud save sync (Google Play version only).

**Languages:** the app is translated into 30 languages.

## Building

The app is built with Gradle (Android Gradle Plugin 8, JDK 21):

```
./gradlew :lemuroid-app:assembleFreeDynamicDebug
```

The `freeDynamic` flavor has no Google libraries and downloads cores on demand. Release builds read their signing
data from a `keystore.properties` file (not committed) with `storeFile`, `storePassword`, `keyAlias` and `keyPassword`.

## Credits

- Original project: [Lemuroid](https://github.com/Swordfish90/Lemuroid) by Swordfish90, built on
  [LibretroDroid](https://github.com/Swordfish90/LibretroDroid).
- Emulation cores: [libretro](https://github.com/libretro) cores (GPL and other open source licenses), distributed
  from [PlayRetroCores](https://github.com/branches78/PlayRetroCores), a fork of
  [LemuroidCores](https://github.com/Swordfish90/LemuroidCores) that adds [SwanStation](https://github.com/libretro/swanstation).
  The source code of each core is available in its libretro repository.
- System icons: "FlatUI" theme from [RetroArch assets](https://github.com/libretro/retroarch-assets) by libretro,
  licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/).
- Box art: [libretro thumbnails](https://github.com/libretro-thumbnails/libretro-thumbnails).
