# Warsmash Engine Modernization Analysis

This document captures a practical modernization roadmap focused on four goals:

1. **Modernization** (codebase and platform sustainability)
2. **Compatibility** (hardware/driver/OS resilience)
3. **Performance** (frame-time stability and throughput)
4. **Quality of Life (QoL)** (developer and player ergonomics)

---

## Phase Status

| Phase | Scope | Status |
|-------|-------|--------|
| **A** | Compatibility knobs, diagnostics, docs, CI | **Complete** |
| **B** | Light leak fix, shader normalization, parser consolidation design | **Next** |
| **C** | Parser unification, server hardening, async asset pipeline | Planned |

---

## Current Signals in the Codebase

- The desktop launcher hard-requires GL30/3.3 and full-screen defaults. Phase A
  added command-line flags (`-windowed`, `-fps`, `-vsync/-novsync`, `-msaa`) so
  users can self-tune compatibility.
- The README explicitly documents known technical debt in rendering and data
  loading:
  - known memory leak in the Light system,
  - multiple parser stacks for the same data domains,
  - mixed GLSL versions across subsystems.
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
  interface and one canonical backend. *(Phase B — design; Phase C — implement)*
- Normalize shader targets and enforce one compatibility strategy (or a small
  explicit set). *(Phase B)*
- Introduce package-level ownership boundaries (`render`, `simulation`, `net`,
  `assets`). *(Phase C)*

### 1.3 Observability

- ~~Add lightweight runtime diagnostics for frame pacing and asset-load timing.~~ *(Done — Phase A)*
- ~~Add a startup capability report dump.~~ *(Done — Phase A)*

## 2) Compatibility Recommendations

### 2.1 Startup/runtime compatibility profiles

Implement launch profiles:

- **Safe profile**: reduced effects, conservative GL features, lower MSAA.
- **Balanced profile**: default feature set.
- **High profile**: aggressive quality/perf assumptions.

Phase A delivered the individual flags that make these profiles possible. Named
profiles can be introduced as a convenience layer in Phase B or C.

### 2.2 Graphics fallback strategy

- Keep GL30 path as primary, but design an explicit fallback policy for
  unsupported features. *(Phase C)*
- Add a user-facing message when required extensions fail. *(Phase C)*

### 2.3 Content compatibility

- ~~Maintain explicit docs for supported Warcraft III patch asset layouts.~~ *(Done — Phase A)*
- ~~Add a quick validator command (`-validate`).~~ *(Done — Phase A)*

## 3) Performance Recommendations

### 3.1 Immediate wins (low risk)

- Profile and fix the documented Light-system leak. *(Phase B)*
- ~~Add optional frame cap defaults for laptops/thermals.~~ *(Done — Phase A)*
- ~~Expose anti-aliasing and VSync controls at launch.~~ *(Done — Phase A)*

### 3.2 Medium-term wins

- Reduce per-frame allocations in render and simulation loops. *(Phase C)*
- Add cache stats and hit/miss telemetry for frequently loaded assets. *(Phase C)*
- Move expensive map/asset preparation toward asynchronous loading with progress
  feedback. *(Phase C)*

### 3.3 Server performance hardening

- Revisit login/session token and handshake paths called out as inefficient.
  *(Phase C)*
- Add rate limiting + cheap pre-auth rejection to reduce amplification under
  abuse. *(Phase C)*

## 4) QoL Recommendations

### 4.1 Player QoL

- ~~Document launcher options in README with examples.~~ *(Done — Phase A)*
- ~~Add `-help` command for discoverability.~~ *(Done — Phase A)*
- ~~Support deterministic debug launch templates.~~ *(Done — Phase A)*

### 4.2 Developer QoL

- ~~Add `CONTRIBUTING.md` with profiling workflow and coding conventions.~~ *(Done — Phase A)*
- Add smoke tests for startup, asset discovery, and one map load scenario.
  *(Phase C)*
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

**Duration:** 2–4 weeks | **Status:** Next

Phase B targets the three highest-impact technical debt items: the Light-system
memory leak, the GLSL version mismatch across subsystems, and a design document
for parser consolidation.

### B.1 Light System Memory Leak Fix

**Goal:** eliminate the accumulation of orphaned `LightInstance` objects so that
memory growth flattens after the initial map load.

**Root cause analysis:**

The light lifecycle is managed by `MdxComplexInstance` and `Scene`:

- `LightInstance.updateVisibility()` calls `scene.addLight(this)` when visible
  and `scene.removeLight(this)` when hidden.
- `Scene.removeInstance()` calls `instance.removeLights(this)`, which properly
  unregisters all lights from `W3xSceneWorldLightManager.lights`.
- However, `Scene.update()` prunes culled instances by removing them directly
  from the `instances` / `batchedInstances` lists *without* calling
  `removeInstance()`, so `removeLights()` is never invoked on the culled
  instance. Its `LightInstance`s remain in the light manager indefinitely.

**Key files:**

| File | Relevance |
|------|-----------|
| `core/…/viewer5/handlers/mdx/LightInstance.java` | `updateVisibility()` add/remove |
| `core/…/viewer5/handlers/mdx/MdxComplexInstance.java` | `updateLights()`, `removeLights()` |
| `core/…/viewer5/Scene.java` | `removeInstance()` vs `update()` prune paths |
| `core/…/viewer5/handlers/w3x/W3xSceneWorldLightManager.java` | Stores active lights |

**Fix approach:**

1. In `Scene.update()`, before removing pruned instances from the lists, call
   `removeLights(this)` on each one so their lights are unregistered.
2. Add a guard in `W3xSceneWorldLightManager.remove()` to handle double-removal
   gracefully (idempotent).
3. Add a diagnostic counter that logs the light-manager size periodically
   (extend `FramePacingTracker` or add a parallel tracker) to verify the fix.

**Validation:**

- Load a map with point-light sources (torches, buildings). Run for 30+ minutes.
- Compare `LightInstance` count before and after the fix using heap snapshots or
  the new diagnostic counter.
- Frame-time 95th/99th percentile should stabilise instead of drifting upward.

### B.2 Shader Target Normalization

**Goal:** unify GLSL version directives so all shaders use one compatible
baseline, eliminating driver warnings and reducing maintenance burden.

**Current state:**

| GLSL version | Subsystem | Source files |
|--------------|-----------|-------------|
| `#version 120` | MDX model rendering | `MdxShaders.java` |
| `#version 330 core` | Terrain, cliffs, water, shadows | `TerrainShaders.java`, `DynamicShadowManager.java` |
| `#version 450 core` | Test shaders | `WarsmashTestGame2.java`, `WarsmashTestGame3.java` |

The MDX shaders at GLSL 120 predate the core-profile requirement. The terrain
shaders at 330 core match the GL 3.3 requirement in `DesktopLauncher`. The test
shaders at 450 core exceed the minimum by two major versions.

**Approach:**

1. **Target baseline:** `#version 330 core` — matches the existing GL 3.3
   requirement and the terrain/shadow shaders.
2. **MDX shaders (120 → 330 core):**
   - Replace deprecated built-ins (`gl_ModelViewProjectionMatrix`,
     `attribute`/`varying`) with `uniform`, `in`/`out`.
   - Replace `texture2D()` with `texture()`.
   - Verify bone-texture sampling still works with `texelFetch` if used.
   - Test with representative MDX models (hero glow, particle emitters,
     ribbons).
3. **Test shaders (450 → 330 core):**
   - Lower the `#version` directive; these shaders are simple and use no
     450-specific features.
4. **Validation:**
   - Run CI (no GL context needed for compile step, but a headless GL smoke
     test could be added in Phase C).
   - Manual test on an NVIDIA and an Intel/Mesa driver to confirm no regressions.

### B.3 Parser Consolidation Design

**Goal:** produce a design document (not implementation) for unifying the
duplicate SLK and INI parsers behind a single interface.

**Current inventory:**

| Format | Viewer parser | ReteraModelStudio parser |
|--------|--------------|------------------------|
| SLK | `SlkFile.java` (`util/`) | `DataTable.readSLK()` (`units/`) |
| INI | `IniFile.java` (`util/`) | `DataTable.readTXT()` (`units/`) |

The viewer parsers (`SlkFile`, `IniFile`) feed into `MappedData` and are used
for splat data, anim sounds, and other viewer-level data. The ReteraModelStudio
parsers (`DataTable.readSLK/readTXT`) produce `Element`/`LMUnit` objects and
power the high-level unit data, ability, and terrain APIs.

**Design deliverable should cover:**

1. A unified interface (`TableDataSource` or similar) that both code paths can
   call.
2. Which parser backend to keep (recommendation: the `DataTable` backend, as it
   is richer and already powers the unit data API).
3. An adapter layer so `MappedData` callers can migrate incrementally.
4. Migration order: which callers to move first (lowest risk → highest risk).
5. Test strategy: comparison of parsed output between old and new paths for a
   set of reference SLK/INI files.

This design will be implemented in Phase C.

---

## Phase C — Implementation & Hardening (Planned)

**Duration:** 4–8 weeks

| Item | Description |
|------|-------------|
| Parser unification | Implement the Phase B design; migrate all callers to one SLK and one INI parser |
| Server hardening | Rate limiting, pre-auth rejection, session-token efficiency |
| Async asset pipeline | Move map/asset loading to background threads with progress feedback |
| Per-frame allocation reduction | Profile and eliminate unnecessary heap allocations in render/simulation loops |
| Package ownership boundaries | Enforce layer separation (`render`, `simulation`, `net`, `assets`) |
| Named launch profiles | `-profile safe/balanced/high` convenience presets |
| GL fallback strategy | Graceful degradation when GL features are unavailable |
| Smoke tests | Automated startup + asset-discovery + map-load tests |

---

## Success Metrics

- Startup crash rate reduction on low-end/older GPUs.
- Lower frame-time variance (95th/99th percentile) on representative maps.
- Reduced memory growth over 30+ minute sessions (target: flat after initial
  load once Phase B light leak is fixed).
- Faster issue triage due to better startup diagnostics and standardized launch
  knobs.
- Zero GLSL-version driver warnings after Phase B shader normalization.
