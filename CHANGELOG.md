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

## Campaign Map Startup Reliability Pass (2026-03-06)

### fix
- **Campaign/skirmish startup failure handling hardened**: `MenuUI` now validates map preloading before hiding menu navigation state for campaign mission launches and direct `startMap(...)` calls. Failed preloads no longer proceed into a broken transition path; they now show `NETERROR_MAPFILEINCOMPLETE` and keep the menu usable.
- **Unified guarded map-config preload path**: a new `tryLoadAndCacheMapConfigs(...)` helper centralizes exception-safe handling for `loadAndCacheMapConfigs(...)`, covering both checked `IOException` and runtime parse/load failures encountered during map config/JASS loading.

## Phase D — Async Loading Pipeline Completion (2026-03-02)

### perf
- **Threaded map prefetch stage added**: `War3MapViewer` now exposes
  `createAsyncMapLoader(...)`, which performs tileset data-source setup and map
  parse/object-data preload (`w3e`, `wpm`, modifications) on a background
  worker thread before handing off to the existing render-thread map loader.
- **Blended loading progress**: loading completion ratio now reflects both the
  async prefetch stage and the render-thread loading-task stage, yielding more
  representative progress feedback on the loading screen.

### fix
- **Menu loading flow migrated to async loader**: `MenuUI` now uses
  `War3MapViewer.AsyncMapLoader` for map startup and closes loader resources on
  both success and failure paths to avoid lingering worker-thread resources.
- **Synchronous compatibility preserved**: existing `MapLoader` creation remains
  available (including the terrain editor flow), while optionally consuming
  preloaded async results when present.

### test
- Added `AsyncLoadCoordinatorTest` (5 tests) covering weighted progress,
  one-time prefetch→main-thread handoff, non-blocking behavior while prefetch
  is pending, failure propagation, and cancellation/close semantics.

### docs
- README and modernization analysis updated to mark Phase D as complete and to
  document the new async loading pipeline behavior.

## Performance/QoL/Docs Polish Pass (2026-03-02)

### perf
- **Lightning effect batch buffer accounting fixed**:
  `LightningEffectBatch` now correctly treats index-buffer capacity as a count
  of `short` elements (not bytes), uploads GL element buffers using byte sizes,
  and skips index-buffer rebuild/upload work when lightning instance count is
  unchanged across frames.
- **Hot-path reach checks avoid sqrt**: `CUnit.canReach(x, y, range)` now uses
  collision-aware squared-distance comparison for the non-pathing fast path.
  This removes repeated scalar sqrt work from high-frequency range queries.

### fix
- **Pathfinding edge bounds guard**: `CPathfindingProcessor` goal-neighborhood
  iteration now correctly uses `j < searchGraph.length` (not `<=`), preventing
  a potential out-of-range access near map boundaries.

### qol
- **Launcher override semantics made deterministic**:
  - profile presets are applied first;
  - explicit flags then override (`-window`, `-fps`, `-vsync/-novsync`, `-msaa`);
  - `-msaa 0` now explicitly disables MSAA.
- **`-help` is now display-independent**: launcher help exits before desktop
  display probing, allowing CLI help usage in headless/CI Linux environments.

### docs
- README reorganized with a clearer quick-start path, updated launcher flag
  behavior, and a tighter status/priority snapshot.
- `docs/ENGINE_MODERNIZATION_ANALYSIS.md` updated to include this polish pass in
  Phase D status tracking and to mark asset cache telemetry as complete.
- `docs/COMPATIBILITY.md` refreshed: removed stale fixed issues and added
  current guidance for 1.30/1.31 data-layout caveats and headless launcher use.

## Phase D — Finalization Pass (2026-03-02)

### fix
- **Canonical parser path for runtime mapped tables**: `MappedData.load(String)`
  no longer routes through legacy `SlkFile` / `IniFile`. It now parses with
  `DataTable.readSLK/readTXT` and ingests through `DataTableSource`, completing
  parser unification for terrain/splat/anim-sound table loads that flow through
  `MappedData`.
- **MappedData SLK type compatibility preserved**: when loading SLK buffers,
  `MappedData` now coerces canonical string cells back to legacy-compatible
  primitive types (`Float` and `Boolean`) so existing `MappedDataRow` numeric
  consumers continue to behave as before.
- **Event-object numeric parsing hardened**: `EventObjectEmitterObject` no longer
  blindly casts SLK fields to `Float`/`Number`; it now accepts both numeric and
  string-backed values, preventing `ClassCastException` risk if data source
  formats differ.
- **Destructable metadata SLK load fix**: `War3MapViewer.loadSLKs()` now applies
  `Units\\DestructableMetaData.slk` to `destructableMetaData` (previously it
  accidentally reloaded `DestructableData.slk` into that table).

### perf
- **ObjectPool wired into simulation allocations**:
  - `CWorldCollision.enumUnitsInRect`, `enumCorpsesInRect`, and
    `enumUnitsOrCorpsesInRect` now reuse pooled scratch `Set<CUnit>` instances
    instead of allocating a new `HashSet` on every call.
  - `CSimulation.update()` now uses a pooled scratch `Set<CTimer>` for duplicate
    timer validation instead of per-tick allocation.
  All pooled paths use `try/finally` acquire/release for exception-safe reuse.

### test
- `TableDataSourceTest` extended with
  `slkBooleansAndIntegersRemainTypedInMappedData`, asserting canonical-parser
  `MappedData` still exposes typed `Boolean` and numeric values for SLK cells.

### docs
- Updated README and modernization roadmap status for Phase D:
  parser remaining-caller migration and ObjectPool wiring now marked complete;
  async asset pipeline remains the primary pending Phase D item.

---

## Phase D — Implementation & Hardening (2026-03-02)

### perf
- **SimulationBudgetTracker wired**: `War3MapViewer.update()` now wraps each
  `CSimulation.update()` call with `SimulationBudgetTracker.beginTick()` /
  `endTick()`. Per-tick timing is reported every ~60 s with avg/max and overrun
  percentage.
- **Asset cache telemetry**: New `AssetCacheTelemetry` class instruments both
  cache paths in `ModelViewer` (`load()` and `loadGeneric()`). Hit/miss counts
  and hit-rate percentage are logged to stdout every 50 cache misses via
  `[AssetCache]` lines.

### compat
- **GL version guard**: `StartupDiagnostics.checkGLRequirements()` is now called
  from `WarsmashGdxMultiScreenGame.create()` immediately after the capability
  report. If the driver reports OpenGL < 3.3 the engine prints a user-readable
  error message (detected vs. required version, plus driver update suggestions)
  and exits with code 1. Requires no user action on supported hardware.
- **Named launch profiles**: `DesktopLauncher` accepts a new `-profile <name>`
  flag with three presets:
  - `safe` — windowed 1280×720, no MSAA, vsync on, 60 fps cap.
  - `balanced` — windowed 1280×720, 2× MSAA, vsync on, 60/30 fps cap.
  - `high` — fullscreen, 4× MSAA, vsync on, uncapped.
  Individual flags (`-window`, `-msaa`, `-fps`, etc.) still override the profile.
  Profile selection is announced in the startup log.

### fix
- **Server: O(n) disconnection lookup eliminated**: `GamingNetworkServerBusinessLogicImpl`
  previously iterated all active sessions to find the one matching a disconnected
  writer — an O(n) scan that became a DDoS amplification vector under load. A
  new `writerToSession` reverse-map is now kept in sync at `login()` /
  `killSession()`, reducing disconnection handling to O(1).
- **Server: login and account-creation rate limiting**: New `LoginRateLimiter`
  tracks failed auth attempts per remote address using a 60-second sliding window.
  After 5 failures the address is blocked for 5 minutes; subsequent requests are
  rejected before touching user storage. The block is announced in the server log
  with address, threshold, and cooldown duration.

### qol
- **Package ownership markers**: `package-info.java` files added for the four
  principal architecture layers — `render` (`viewer5`), `simulation`
  (`simulation`), `assets` (`datasources`), and `net` (`networking`) — documenting
  allowed and forbidden cross-layer dependencies.

### test
- `ObjectPoolTest` (8 tests): acquire/release round-trip, overflow behaviour,
  hit-rate calculation, `resetStats()` isolation.
- `SimulationBudgetTrackerTest` (5 tests): begin/end semantics, no-throw on
  zero-duration ticks, report-interval boundary at 3 600 ticks.
- `StartupDiagnosticsTest` (9 tests): `parseGLVersion()` against NVIDIA, Intel
  DCH, Mesa, ATI, and degenerate inputs.
- `AssetCacheTelemetryTest` (7 tests): hit/miss counting, hit-rate, reset, and
  periodic-report no-throw.

---

## Phase D — Parser Unification Kickoff (2026-03-02)

### fix
- Added a new read-only parser abstraction:
  `com.etheller.warsmash.util.table.TableDataSource`.
- Added adapter implementations for all current table stacks:
  - `SlkFileDataSource` (legacy `SlkFile`)
  - `IniFileDataSource` (legacy `IniFile`)
  - `DataTableSource` (canonical `DataTable`)
- Refactored `MappedData` to load through `TableDataSource` adapters instead of
  directly binding to parser implementations. This unifies the first caller onto
  the Phase D parser interface without changing existing runtime behavior.

### test
- Added `TableDataSourceTest` with:
  - SLK parity checks between `SlkFileDataSource` and `DataTableSource`
  - INI parity checks between `IniFileDataSource` and `DataTableSource`
  - A compatibility assertion that `MappedData` still exposes typed numeric
    SLK values needed by existing emitter-loading code paths

---

## Phase C — Render Hot-Path Performance (2026-03-02)

### perf
- **Light-data per-frame cache**: `LightInstance` now caches its packed 16-float
  GPU block in a `float[] cache` field guarded by a static generation counter.
  `LightInstance.advanceGeneration()` is called once per frame by
  `W3xSceneWorldLightManager.update()`. Subsequent `bind()` calls within the
  same frame bulk-copy from the cache via `FloatBuffer.put(float[], 0, 16)`
  instead of re-evaluating all keyframe tracks. Halves the number of keyframe
  sampler calls for every active point light regardless of how many GPU textures
  it is written into.
- **Separate unit/terrain light buffers**: `W3xSceneWorldLightManager` now owns
  a dedicated `unitLightBuffer` and `terrainLightBuffer`. Both are populated in
  a single loop over `this.lights`, eliminating the shared-buffer `clear()`/reuse
  pattern that forced sequential uploads and obscured buffer ownership.
- **Bone texture bulk copy**: `MdxComplexInstance.updateBoneTexture()` replaced
  16 absolute-indexed `FloatBuffer.put(int, float)` calls per bone with a single
  `FloatBuffer.put(float[], 0, 16)` bulk copy (JVM maps this to native `memcpy`).
  A trailing `flip()` correctly positions the buffer for `DataTexture.bindAndUpdate`.
  For a model with 80 bones this reduces per-frame JNI scalar writes from 1,280
  to 80 bulk copies.
- **Frame-pacing p95/p99**: `FramePacingTracker.report()` now sorts a copy of
  its ring buffer and includes the 95th- and 99th-percentile frame times in the
  60-second summary line. A spike-detection warning fires when p99 exceeds 3×
  the window average. Zero per-frame overhead — sorting happens only during the
  periodic report.
- **`ObjectPool<T>`**: New `com.etheller.warsmash.util.ObjectPool<T>` — a
  fixed-capacity stack-backed pool with `acquire()`/`release()` semantics and a
  `hitRate()` diagnostic. Provides the infrastructure for reducing GC pressure in
  particle-emission and simulation-allocation hot paths in Phase D.
- **`SimulationBudgetTracker`**: New `com.etheller.warsmash.util.SimulationBudgetTracker`
  measures wall time around any simulation block via `beginTick()`/`endTick()`.
  Reports avg/max per-tick cost, configured budget (default 8 ms), and overrun
  count every ~60 s. Ready to be wired around `CSimulation.step()` in Phase D.

### docs
- **OpenMW-equivalent vision**: README rewritten to open with Warsmash's long-term
  goal of being the OpenMW for Warcraft III. Roadmap table extended to include
  Phases C–F.
- **ENGINE_MODERNIZATION_ANALYSIS.md** updated: Phase C deliverables documented
  in detail; Phases D (parser unification, async pipeline, server hardening),
  E (scripting, map format), and F (modding layer) added with item lists.

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
