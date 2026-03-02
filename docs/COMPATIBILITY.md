# Warsmash Compatibility Reference

This document summarises known-good hardware and software configurations, known
limitations, and troubleshooting tips for running Warsmash.

---

## Minimum Requirements

| Component | Minimum |
|-----------|---------|
| Java | 17 (Eclipse Temurin recommended; see note below) |
| OpenGL | 3.3 Core Profile |
| OS | Windows 10+, Ubuntu 20.04+, macOS 12+ |
| RAM | 2 GB |
| GPU VRAM | 512 MB |

> **Java note:** The `openjdk-17` package from some Ubuntu apt mirrors ships
> with unusual native-library paths that cause a crash on startup. Use
> [Eclipse Temurin 17](https://adoptium.net/) instead.

---

## Tested Configurations

| GPU / Driver | OS | OpenGL | Java | Status | Notes |
|---|---|---|---|---|---|
| NVIDIA RTX 30xx, driver 550+ | Windows 11 | 4.6 | 17, 21 | ✅ Works | — |
| NVIDIA GTX 10xx, driver 470+ | Windows 10 | 4.6 | 17 | ✅ Works | — |
| AMD RX 6000, Adrenalin 24.x | Windows 11 | 4.6 | 17 | ✅ Works | — |
| Intel UHD 620, driver 31.x | Windows 10 | 4.6 | 17 | ✅ Works | Lower performance |
| NVIDIA GTX 970, driver 390 | Ubuntu 20.04 | 4.6 | 17 | ✅ Works | Temurin JDK required |
| Mesa (llvmpipe) | Ubuntu 22.04 | 3.3 | 17 | ⚠️ Partial | Software renderer; very slow |
| Apple M1 (Rosetta) | macOS 13 | 4.1 | 17 | ⚠️ Partial | macOS caps at GL 4.1; audio may differ |

*If you test on a configuration not listed here, please open an issue or PR to
add your results.*

---

## Warcraft III Asset Compatibility

| WC3 Patch | Data Layout | Warsmash Support | Notes |
|---|---|---|---|
| 1.22 – 1.28 | MPQ archives | ✅ Supported | Classic assets; set `MaxPlayers=16` |
| 1.29 | MPQ (no War3Patch.mpq) | ✅ Supported | Remove `War3Patch.mpq` from INI; `MaxPlayers=28` |
| 1.30 | Manually extracted CASC folders | ⚠️ Untested recently | May have regressions from newer code |
| 1.31 | `.w3mod` extracted folders | ⚠️ Untested recently | May have regressions from newer code |
| 1.32 (Reforged) | CASC with prefix notation | ✅ Supported | Best-tested modern patch; see README for HD notes |
| 1.33+ | New MDX format | ❌ Not supported | Model format not yet parsed |

---

## Known Issues and Workarounds

### Patch 1.30 / 1.31 data layouts
These patches are still supported in principle, but they are tested less
frequently than 1.22–1.29 and 1.32.10. If you use extracted `.mpq`/`.w3mod`
folder layouts for these versions, regressions are possible after unrelated
engine work. Please report map/asset edge cases with your startup capability
report.

### Headless/CI environments (Linux)
Desktop launch paths that initialize LWJGL display mode require an active
display server (X11/Wayland). For headless checks, use non-graphical launcher
paths such as:

```bash
./gradlew :desktop:runGame -Pargs="-help -nolog"
./gradlew :desktop:runGame -Pargs="-validate -nolog"
```

These commands validate launcher/config behavior without opening a game window.

### FLAC Audio Quality (Patch 1.32+)
Warcraft III Reforged stores all audio as FLAC. LibGDX does not natively decode
FLAC, so Warsmash converts on-the-fly using a bundled pure-Java FLAC decoder.
This decoder drops some precision bytes to fit LibGDX's WAV pipeline; audio may
sound slightly "tinny" compared to playing the files in an external player.

### DDS Texture Gamma (Patch 1.32+)
Reforged stores textures in DDS format. An sRGB correction that was required for
BLP textures is not needed for DDS. On older builds of Warsmash this correction
was sometimes applied to DDS textures, making them appear very dark. Should be
fixed in recent code, but worth verifying if textures look unusually dark.

---

## Troubleshooting

### Startup crash / black window

1. Run with `-validate` to confirm your `warsmash.ini` data source paths exist:
   ```
   ./gradlew desktop:runGame -Pargs="-validate"
   ```
2. Check `Logs/*.err.log` (created in the working directory unless `-nolog` is
   passed) for the Java stack trace.
3. Try running windowed with low settings to isolate a fullscreen/MSAA driver
   bug:
   ```
   ./gradlew desktop:runGame -Pargs="-window 1280 720 -msaa 0 -novsync"
   ```

### No audio

OpenAL is required. On Linux, install `libopenal1` (Debian/Ubuntu) or the
equivalent for your distribution.

### Very low FPS

Cap the frame rate and disable VSync as a starting point:
```
./gradlew desktop:runGame -Pargs="-window 1280 720 -fps 30 -novsync"
```

---

## Startup Capability Report

At every launch, Warsmash prints a capability report to stdout/log:

```
=== Warsmash Startup Capability Report ===
Java:        17.0.10 (OpenJDK 64-Bit Server VM)
OS:          Linux 6.1.0 [amd64]
GL Vendor:   NVIDIA Corporation
GL Renderer: NVIDIA GeForce RTX 3070/PCIe/SSE2
GL Version:  4.6.0 NVIDIA 550.78
GLSL:        4.60 NVIDIA
Display:     1920x1080 @ 60 Hz
==========================================
```

Include this block when reporting bugs — it greatly speeds up triage.
