# Changelog

All notable changes to Warsmash are documented here.

Changes are grouped by category:

| Category | Meaning |
|---|---|
| `compat` | Hardware/driver/OS compatibility |
| `perf` | Performance or memory |
| `qol` | Quality of life (player or developer) |
| `render` | Rendering correctness or visuals |
| `fix` | General bug fix |
| `break` | Breaking change requiring user action |

---

## [Unreleased]

### qol
- Added startup capability report: GL vendor/renderer/version, GLSL version,
  display resolution, Java version, and OS info are printed to stdout at launch.
- Added frame-pacing diagnostics: min/max/average frame time and effective FPS
  are logged to stdout every 60 seconds.
- Added `-validate` / `--validate` launcher flag: checks that every data source
  path declared in `warsmash.ini` exists on disk and exits with a pass/fail
  summary without starting the game window.
- Added `-help` launcher flag (and `--help`, `-h` aliases) to print all
  available command-line options and exit.
- Added `-window` / `-windowed [width height]` launcher flag to start in
  windowed mode instead of fullscreen (defaults to 1280×720).
- Added `-vsync` / `-novsync` launcher flags to force VSync on or off.
- Added `-fps <value>` launcher flag to cap foreground and background frame rate
  (`0` = uncapped).
- Added `-msaa <samples>` launcher flag to control MSAA sample count.
- Added `-ini <path>` launcher flag to specify a custom INI file.
- Added `-loadfile <path>` launcher flag to auto-load a map or TOC file.
- Added `-nolog` launcher flag to keep stdout/stderr on the console instead of
  writing to `Logs/` files.
- Added `CONTRIBUTING.md` with coding conventions, architecture overview, and
  profiling workflow.
- Added `docs/COMPATIBILITY.md` with tested GPU/OS/driver configurations,
  known issues, and troubleshooting steps.
- Added GitHub Actions CI workflow: compiles all subprojects on Ubuntu and
  Windows against Java 17 and 21.

### compat
- Added `docs/COMPATIBILITY.md` documenting supported Warcraft III patch asset
  layouts, known caveats, and minimum hardware/software requirements.
- Upgraded Gradle wrapper from 7.3.3 to 8.6 for Java 21 compatibility.
- Upgraded `org.beryx.runtime` Gradle plugin from 1.12.5 to 1.13.1.

---

## Pre-changelog history

Earlier development history is tracked via git log. See `git log --oneline`
for a summary of changes before this changelog was introduced.
