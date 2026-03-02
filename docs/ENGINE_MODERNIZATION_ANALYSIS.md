# Warsmash Engine Modernization Analysis

## Vision: The OpenMW for Warcraft III

Warsmash's long-term goal is to be for Warcraft III what [OpenMW](https://openmw.org/) is for
Morrowind: a fully open, community-maintained reimplementation of the game engine that lets
anyone run WC3 maps and mods on a modern, portable, hackable platform — forever.

OpenMW took roughly 15 years from first commit to full feature parity. Warsmash is on a
similar trajectory. The phases below describe the structured path toward that goal, organized
around four recurring concerns:

1. **Modernization** — codebase and platform sustainability
2. **Compatibility** — hardware/driver/OS resilience
3. **Performance** — frame-time stability and throughput
4. **Quality of Life (QoL)** — developer and player ergonomics

---

## Phase Status

| Phase | Scope | Status |
|-------|-------|--------|
| **A** | Compatibility knobs, diagnostics, docs, CI | **Complete** |
| **B** | Light leak fix, shader normalization, parser consolidation design | **Complete** |
| **C** | Per-frame allocation reduction, light-data caching, simulation instrumentation | **Complete** |
| **D** | Parser unification, server hardening, async asset pipeline | In Progress |
| **E** | Full JASS/Lua scripting, map format support to 1.32, multiplayer hardening | Planned |
| **F** | Community modding layer, asset-override system, mod manager API | Planned |

---

## Current Signals in the Codebase

- The desktop launcher hard-requires GL30/3.3 and full-screen defaults. Phase A
  added command-line flags (`-windowed`, `-fps`, `-vsync/-novsync`, `-msaa`) so
  users can self-tune compatibility.
- The README now explicitly documents the OpenMW-equivalent long-term vision.
- Server business logic has comments acknowledging potential inefficiency and
  DDoS sensitivity in a hot path.

---

## 1) Modernization Recommendations

### 1.1 Build and dependency modernization

- ~~Upgrade Gradle wrapper and LibGDX to current stable baselines.~~ *(Done — Phase A)*
- ~~Add CI matrix for Linux + Windows + Java 17/21.~~ *(Done — Phase A)*
- ~~Add an explicit compatibility table in docs.~~ *(Done — Phase A)*

### 1.2 Technical debt reduction

- Consolidate duplicate parser implementations (SLK/INI variants) behind one
  interface and one canonical backend. *(Phase D — design ✓ from Phase B; implement in D)*
- ~~Normalize shader targets and enforce one compatibility strategy.~~ *(Phase B ✓)*
- Introduce package-level ownership boundaries (`render`, `simulation`, `net`,
  `assets`). *(Phase D)*

### 1.3 Observability

- ~~Add lightweight runtime diagnostics for frame pacing and asset-load timing.~~ *(Done — Phase A)*
- ~~Add a startup capability report dump.~~ *(Done — Phase A)*
- ~~Add per-frame p95/p99 percentile tracking.~~ *(Done — Phase C)*
- ~~Add simulation-tick budget tracking and overrun reporting.~~ *(Done — Phase C)*

---

## 2) Compatibility Recommendations

### 2.1 Startup/runtime compatibility profiles

Implement launch profiles:

- **Safe profile**: reduced effects, conservative GL features, lower MSAA.
- **Balanced profile**: default feature set.
- **High profile**: aggressive quality/perf assumptions.

Phase A delivered the individual flags that make these profiles possible. Named
profiles can be introduced as a convenience layer in Phase D.

### 2.2 Graphics fallback strategy

- Keep GL30 path as primary, but design an explicit fallback policy for
  unsupported features. *(Phase D)*
- Add a user-facing message when required extensions fail. *(Phase D)*

### 2.3 Content compatibility

- ~~Maintain explicit docs for supported Warcraft III patch asset layouts.~~ *(Done — Phase A)*
- ~~Add a quick validator command (`-validate`).~~ *(Done — Phase A)*

---

## 3) Performance Recommendations

### 3.1 Immediate wins (low risk)

- ~~Profile and fix the documented Light-system leak.~~ *(Phase B ✓)*
- ~~Add optional frame cap defaults for laptops/thermals.~~ *(Done — Phase A)*
- ~~Expose anti-aliasing and VSync controls at launch.~~ *(Done — Phase A)*

### 3.2 Medium-term wins

- ~~Reduce per-frame allocations in render and simulation loops.~~ *(Phase C ✓ — see below)*
- Add cache stats and hit/miss telemetry for frequently loaded assets. *(Phase D)*
- Move expensive map/asset preparation toward asynchronous loading with progress
  feedback. *(Phase D)*

### 3.3 Server performance hardening

- Revisit login/session token and handshake paths called out as inefficient.
  *(Phase D)*
- Add rate limiting + cheap pre-auth rejection to reduce amplification under
  abuse. *(Phase D)*

---

## 4) QoL Recommendations

### 4.1 Player QoL

- ~~Document launcher options in README with examples.~~ *(Done — Phase A)*
- ~~Add `-help` command for discoverability.~~ *(Done — Phase A)*
- ~~Support deterministic debug launch templates.~~ *(Done — Phase A)*

### 4.2 Developer QoL

- ~~Add `CONTRIBUTING.md` with profiling workflow and coding conventions.~~ *(Done — Phase A)*
- Add smoke tests for startup, asset discovery, and one map load scenario.
  *(Phase D)*
- ~~Add a changelog category structure.~~ *(Done — Phase A)*

---

## Phase A — Complete

**Duration:** 1–2 weeks | **Status:** Complete

All Phase A deliverables have been merged. See `CHANGELOG.md` for the full list.

### Deliverables

| Item | Details |
|------|---------|
| Startup capability report | `StartupDiagnostics.java` — GL vendor/renderer/version, GLSL, Java, OS |
| Frame-pacing diagnostics | `FramePacingTracker.java` — logged every 60 s |
| Launcher flags | `-validate`, `-help`, `-window`, `-vsync/-novsync`, `-fps`, `-msaa`, `-ini`, `-loadfile`, `-nolog` |
| Compatibility docs | `docs/COMPATIBILITY.md` — tested configs, known issues, troubleshooting |
| Contributing guide | `CONTRIBUTING.md` — conventions, architecture, profiling |
| CI pipeline | `.github/workflows/ci.yml` — Ubuntu + Windows × Java 17 + 21 |
| Gradle upgrade | 7.3.3 → 8.6; `org.beryx.runtime` 1.12.5 → 1.13.1 |
| Changelog | `CHANGELOG.md` with category structure |

---

## Phase B — Stability & Shader Normalization

**Duration:** 2–4 weeks | **Status:** Complete

### B.1 Light System Memory Leak Fix *(Complete)*

**Root cause:** `Scene.update()` pruned culled instances from the visible list
without calling `removeLights()`, leaving orphaned `LightInstance` objects
registered in `W3xSceneWorldLightManager` indefinitely.

**Fix:** `Scene.update()` now calls `instance.removeLights(this)` on every
pruned instance before removing it. `W3xSceneWorldLightManager.remove()` is
idempotent (safe against double-removal). A diagnostic counter logs the active
light count every ~60 s for verification without a heap profiler.

### B.2 Shader Target Normalization *(Complete)*

All active shaders now use `#version 330 core`, matching the GL 3.3 requirement
declared in `DesktopLauncher`. MDX HD shaders (`vsHd`/`fsHd`) were upgraded from
`#version 120`; test shaders were lowered from `#version 450 core`.

### B.3 Parser Consolidation Design *(Complete)*

Design document: `docs/PARSER_CONSOLIDATION_DESIGN.md`. Implementation deferred
to Phase D.

### Phase B Deliverables Summary

| Item | Details |
|------|---------|
| Light-system leak fix | `Scene.update()` calls `removeLights(scene)` on pruned instances |
| Light-manager guard | `W3xSceneWorldLightManager.remove()` is idempotent; logs active light count every ~60 s |
| MDX HD shader upgrade | `vsHd` / `fsHd` — `#version 120` → `330 core`; `attribute`/`varying` → `in`/`out`; `texture2D` → `texture`; `gl_FragColor` → explicit `out vec4 fragColor` |
| Bone-texture 330 helper | `MdxShaders.BONE_TEXTURE_330` — `#version 330 core`-compatible inline version used by `vsHd` |
| Transforms upgrade | `Shaders.transforms` — `attribute` → `in` (used exclusively by `vsHd`) |
| Test-shader version | `WarsmashTestGame2` / `WarsmashTestGame3` — `#version 450 core` → `330 core` |
| Parser consolidation design | `docs/PARSER_CONSOLIDATION_DESIGN.md` |

---

## Phase C — Render Hot-Path Performance

**Duration:** 2–3 weeks | **Status:** Complete

Phase C targeted the per-frame CPU cost in the render pipeline, establishing
the infrastructure (object pooling, budget tracking) needed for ongoing
performance work.

### C.1 LightInstance Per-Frame Data Cache *(Complete)*

**Problem:** `W3xSceneWorldLightManager.update()` packed the GPU light texture
in two passes over `this.lights` (once for unit lights, once for terrain lights).
Each pass called `LightInstance.bind()`, which sampled 6 keyframe tracks and
wrote 16 floats to a direct FloatBuffer — all work that was identical between
the two passes.

**Fix:** Added a 16-element `float[] cache` and an integer `cacheGeneration`
to `LightInstance`. A static `currentGeneration` counter is incremented once
per frame by `LightInstance.advanceGeneration()` (called at the top of
`W3xSceneWorldLightManager.update()`). `bind()` recomputes keyframes and fills
the cache on the first call within a generation; subsequent calls within the
same generation use `FloatBuffer.put(float[], 0, 16)` to bulk-copy the cached
data, which maps to a native `memcpy` and skips all keyframe evaluation.

**Result:** Keyframe-sampler calls per frame per light reduced from 2× to 1×,
regardless of how many GPU textures the light is written into.

### C.2 Separate Unit/Terrain Buffers *(Complete)*

**Problem:** The old code reused a single `lightDataCopyHeap` FloatBuffer for
both the unit and terrain light textures, requiring an explicit `clear()` between
passes and forcing sequential uploads.

**Fix:** `W3xSceneWorldLightManager` now maintains separate `unitLightBuffer`
and `terrainLightBuffer` objects. Both are filled in a single loop over
`this.lights`, interleaving the writes. The light data for each point light is
written twice, but the second write is now a cache-hit bulk copy (see C.1) so
the additional write is cheap.

### C.3 Bone Texture Bulk Copy *(Complete)*

**Problem:** `MdxComplexInstance.updateBoneTexture()` issued 16 individual
absolute-indexed `FloatBuffer.put(int, float)` calls per bone to copy each
world matrix into the GPU bone texture. On a model with 80 bones this is 1,280
JNI-style scalar puts per frame.

**Root cause discovery:** LibGDX's `Matrix4` stores its values in a `float[16]`
array where the constants `M00=0, M10=1, M20=2, ..., M33=15` are contiguous
indices 0–15. The original code therefore wrote `val[0]` through `val[15]` in
order, equivalent to a direct array copy.

**Fix:** Replaced the 16-scalar loop body with a single
`worldMatricesCopyHeap.put(worldMatrix.val, 0, 16)` call, which the JVM
implements via native `memcpy`. A trailing `flip()` positions the buffer
correctly for `bindAndUpdate`.

### C.4 Frame-Pacing p95/p99 *(Complete)*

`FramePacingTracker.report()` now sorts a copy of the ring buffer and emits
95th- and 99th-percentile frame times alongside the existing average and max.
A warning is printed when p99 exceeds 3× the average, flagging sessions with
intermittent spikes. This change adds zero per-frame cost (sorting occurs only
during the 60-second report).

### C.5 ObjectPool&lt;T&gt; *(Complete)*

`com.etheller.warsmash.util.ObjectPool<T>` — a simple fixed-capacity
stack-backed pool with `acquire()`/`release()` semantics and a `hitRate()` stat
for profiling. Available for use by particle systems, simulation allocations,
and other hot paths. No consumer has been wired in Phase C; this is
infrastructure for Phase D and beyond.

### C.6 SimulationBudgetTracker *(Complete)*

`com.etheller.warsmash.util.SimulationBudgetTracker` — wraps `System.nanoTime()`
around any code block and periodically reports avg/max tick time, configured
budget, and overrun percentage. No consumer has been wired in Phase C; designed
to be dropped in around `CSimulation.step()` in Phase D.

### Phase C Deliverables Summary

| Item | Details |
|------|---------|
| Light-data cache | `LightInstance` — `float[] cache`, `cacheGeneration`, `advanceGeneration()` |
| Single-generation advance | `W3xSceneWorldLightManager.update()` — calls `LightInstance.advanceGeneration()` once per frame |
| Separate light buffers | `W3xSceneWorldLightManager` — `unitLightBuffer` + `terrainLightBuffer`, filled in one loop |
| Bone texture bulk copy | `MdxComplexInstance.updateBoneTexture()` — `put(val, 0, 16)` + `flip()` per bone |
| Frame-pacing p95/p99 | `FramePacingTracker` — sorts ring-buffer copy, emits p95/p99, warns when p99 > 3× avg |
| ObjectPool&lt;T&gt; | `util/ObjectPool.java` — stack-backed pool, `acquire()`/`release()`, `hitRate()` |
| SimulationBudgetTracker | `util/SimulationBudgetTracker.java` — ns-resolution tick timer, overrun reporting |

---

## Phase D — Implementation & Hardening (In Progress)

**Duration:** 4–8 weeks

| Item | Description | Status |
|------|-------------|--------|
| Parser unification — interface + adapters | `TableDataSource`, `SlkFileDataSource`, `IniFileDataSource`, `DataTableSource`; `MappedData` migrated | ✓ Done |
| Wire SimulationBudgetTracker | `War3MapViewer` wraps `CSimulation.update()` with `beginTick()`/`endTick()` | ✓ Done |
| Named launch profiles | `-profile safe/balanced/high` in `DesktopLauncher` | ✓ Done |
| GL fallback strategy | `StartupDiagnostics.checkGLRequirements()` — version gate + user-readable error + exit | ✓ Done |
| Server hardening | `LoginRateLimiter` (5 failures / 60 s → 5-min block); O(1) `disconnected()` via `writerToSession` reverse map | ✓ Done |
| Asset cache telemetry | `AssetCacheTelemetry` instruments `ModelViewer.load()` and `loadGeneric()` | ✓ Done |
| Package ownership boundaries | `package-info.java` for `render`, `simulation`, `net`, `assets` layers | ✓ Done |
| Smoke / unit tests | `ObjectPoolTest`, `SimulationBudgetTrackerTest`, `StartupDiagnosticsTest`, `AssetCacheTelemetryTest` | ✓ Done |
| Parser unification — remaining SLK/INI callers | Migrate splat, anim-sound, terrain readers to `TableDataSource` | Pending |
| Wire ObjectPool | Apply `ObjectPool<T>` to particle emitters and simulation allocations | Pending |
| Async asset pipeline | Move map/asset loading to background threads with progress feedback | Pending |

---

## Phase E — Scripting & Map Format (Planned)

| Item | Description |
|------|-------------|
| Full JASS coverage | Implement remaining native JASS APIs in `CSimulation` |
| Lua/JASS 2 support | Foundation for map scripts targeting post-1.31 Lua APIs |
| Map format to 1.32.10 | Reliable loading of all map chunks on supported patches |
| Multiplayer hardening | Authoritative server, deterministic simulation, lag compensation |
| Campaign support | Chain map loading, persistent hero carry-over, campaign screen UI |

---

## Phase F — Community Modding Layer (Planned)

| Item | Description |
|------|-------------|
| Asset override system | Per-map and per-mod texture/model/sound replacement |
| Mod manager API | Declarative mod descriptors; load-order resolution |
| Custom UI scripting | Frame-definition extensions beyond stock FDF |
| Editor integration | Round-trip with World Editor; Warsmash as a preview backend |
| Distribution tooling | Self-contained mod package format (no raw WC3 assets shipped) |

---

## Success Metrics

- Startup crash rate reduction on low-end/older GPUs.
- Lower frame-time variance (95th/99th percentile) on representative maps.
- Flat memory growth over 30+ minute sessions (Phase B light leak addressed; Phase D async pipeline).
- Faster issue triage due to better startup diagnostics and standardized launch knobs.
- Zero GLSL-version driver warnings (Phase B shader normalization complete).
- Simulation budget overrun rate < 5% on reference hardware at 60 fps.
- All WC3 1.22–1.32.10 maps load and play to map-win condition (Phase E target).
