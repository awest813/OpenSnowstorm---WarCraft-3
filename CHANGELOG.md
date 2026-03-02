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

## User-Testing Readiness (2026-03-02)

### fix
- **Particle emitters killed on view-cull**: `MdxComplexInstance.removeLights()`
  previously called `particleEmitter.onRemove()` for every emitter when the
  instance was pruned from the visible list during `Scene.update()`. This tore
  down particle effects any time a unit scrolled off screen. The fix separates
  concerns: `removeLights()` now only deregisters `LightInstance` objects;
  particle-emitter teardown is moved to a new `onInstanceRemoved()` hook that is
  called only from `Scene.removeInstance()` (permanent removal). View-culled
  instances continue to deregister their lights correctly.
- **Ghost batched instances after removal**: `Scene.removeInstance()` removed the
  instance from `this.instances` but not from `this.batchedInstances`. A removed
  batched unit could persist in the render list until the next full frame prune,
  producing a one-frame ghost. `batchedInstances.remove(instance)` is now called
  unconditionally.
- **vsHd GLSL divide-by-zero with empty light texture**: The first light in the
  `vsHd` vertex shader was read unconditionally via `0.5 / u_lightTextureHeight`,
  which evaluates to `+Infinity` in GLSL when no lights are active
  (`u_lightTextureHeight == 0`). The read is now guarded by
  `if (u_lightTextureHeight > 0.5)` and `v_lightDir` is pre-initialised to
  `vec4(0.0)` so the downstream fragment shader receives a safe value when the
  scene has no dynamic lights.
- **Uninitialised `mat4 bone` in vertex-group shader path**: The non-SKIN
  `getVertexGroupMatrix()` function in `Shaders.transforms` declared `mat4 bone;`
  without an initialiser. GLSL 3.30 core does not zero-initialise locals, so bone
  accumulation operated on undefined memory and could produce corrupted vertex
  positions for vertex-group-animated models. Changed to `mat4 bone = mat4(0.0);`.

### qol
- **Paired log file names**: `DesktopLauncher` now captures a single
  `System.currentTimeMillis()` value and uses it for both the `.out.log` and
  `.err.log` filenames, ensuring the two files from the same session share the
  same timestamp prefix.

### test
- Added 3 new unit tests to `MdxShadersTest` covering the shader fixes:
  `vsHd_firstLightIsGuardedByLightCount`, `vsHd_defaultsVLightDirToZero`, and
  `transforms_nonSkinPath_boneMatrixInitialised`. Total test count: 23.

---

## Phase B — Stability & Shader Normalization (2026-03-02)

### fix
- **Light-system memory leak**: `Scene.update()` now calls `removeLights(scene)`
  on every instance pruned from the active list before removing it, so orphaned
  `LightInstance` objects are properly unregistered from
  `W3xSceneWorldLightManager`. Previously these remained in the light manager
  indefinitely, causing unbounded memory growth and frame-time drift on long
  sessions.

### perf
- `W3xSceneWorldLightManager.remove()` is now idempotent: `ArrayList.remove()`
  silently ignores missing elements, preventing spurious state corruption on
  double-removal.
- `W3xSceneWorldLightManager` logs active dynamic light count to stdout every
  ~60 seconds (`[LightManager] active dynamic lights=N`) so the leak fix can be
  verified without a heap profiler.

### render
- **GLSL version normalization**: MDX HD shaders (`vsHd` / `fsHd`) upgraded
  from `#version 120` to `#version 330 core`:
  - `attribute` → `in` (vertex inputs).
  - `varying` → `out` (vertex) / `in` (fragment) for all interpolated
    variables.
  - `texture2D()` → `texture()` for all active sampler calls.
  - `gl_FragColor` replaced by an explicit `out vec4 fragColor` declaration.
  - `Shaders.boneTexture` embedded in `vsHd` updated via a `#version 330
    core`-specific copy (`BONE_TEXTURE_330`) that replaces `texture2D` with
    `texture`.
  - `Shaders.transforms` (used exclusively by `vsHd`) updated: `attribute`
    → `in` for all vertex-input declarations.
- Test shaders in `WarsmashTestGame2` and `WarsmashTestGame3` lowered from
  `#version 450 core` to `#version 330 core` — no 450-specific features were
  used.

### docs
- Added `docs/PARSER_CONSOLIDATION_DESIGN.md`: design document for unifying
  the duplicate SLK/INI parser stacks behind a single `TableDataSource`
  interface (implementation deferred to Phase C).

---

## Phase A — Compatibility, Diagnostics & Documentation (2026-03-02)

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
