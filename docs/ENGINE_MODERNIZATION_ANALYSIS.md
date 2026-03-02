# Warsmash Engine Modernization Analysis

This document captures a practical modernization roadmap focused on four goals:

1. **Modernization** (codebase and platform sustainability)
2. **Compatibility** (hardware/driver/OS resilience)
3. **Performance** (frame-time stability and throughput)
4. **Quality of Life (QoL)** (developer and player ergonomics)

## Current Signals in the Codebase

- The desktop launcher currently hard-requires GL30/3.3 and full-screen defaults, which can be brittle on older or unusual GPU driver stacks. See `DesktopLauncher` configuration.  
- The README explicitly documents known technical debt in rendering and data loading:
  - known memory leak in the Light system,
  - multiple parser stacks for the same data domains,
  - mixed GLSL versions across subsystems.
- Server business logic has comments acknowledging potential inefficiency and DDoS sensitivity in a hot path.

## 1) Modernization Recommendations

### 1.1 Build and dependency modernization

- Upgrade Gradle wrapper and LibGDX to current stable baselines in a dedicated branch.
- Add CI matrix for Linux + Windows + Java 17/21 to catch platform drift early.
- Add an explicit compatibility table in docs (`GPU, driver, OpenGL version, OS`).

### 1.2 Technical debt reduction

- Consolidate duplicate parser implementations (SLK/INI variants) behind one interface and one canonical backend.
- Normalize shader targets and enforce one compatibility strategy (or a small explicit set).
- Introduce package-level ownership boundaries (`render`, `simulation`, `net`, `assets`).

### 1.3 Observability

- Add lightweight runtime diagnostics for frame pacing and asset-load timing.
- Add a startup capability report dump (OpenGL vendor/version/extensions, audio backend info).

## 2) Compatibility Recommendations

### 2.1 Startup/runtime compatibility profiles

Implement launch profiles:

- **Safe profile**: reduced effects, conservative GL features, lower MSAA.
- **Balanced profile**: default feature set.
- **High profile**: aggressive quality/perf assumptions.

In this change set, launcher QoL was extended to help users self-tune compatibility via command line (`-windowed`, `-fps`, `-vsync/-novsync`, `-msaa`, `-help`).

### 2.2 Graphics fallback strategy

- Keep GL30 path as primary, but design an explicit fallback policy for unsupported features.
- Add a user-facing message when required extensions fail.

### 2.3 Content compatibility

- Maintain explicit docs for supported Warcraft III patch asset layouts and known caveats.
- Add a quick validator command that checks `warsmash.ini` asset path health before full launch.

## 3) Performance Recommendations

### 3.1 Immediate wins (low risk)

- Profile and fix the documented Light-system leak first.
- Add optional frame cap defaults for laptops/thermals.
- Expose anti-aliasing and VSync controls at launch (implemented in this change set).

### 3.2 Medium-term wins

- Reduce per-frame allocations in render and simulation loops.
- Add cache stats and hit/miss telemetry for frequently loaded assets.
- Move expensive map/asset preparation toward asynchronous loading with progress feedback.

### 3.3 Server performance hardening

- Revisit login/session token and handshake paths called out as inefficient.
- Add rate limiting + cheap pre-auth rejection to reduce amplification under abuse.

## 4) QoL Recommendations

### 4.1 Player QoL

- Document launcher options in README with examples.
- Add `-help` command for discoverability (implemented in this change set).
- Support deterministic debug launch templates (windowed size + fixed FPS).

### 4.2 Developer QoL

- Add `CONTRIBUTING.md` with profiling workflow and coding conventions.
- Add smoke tests for startup, asset discovery, and one map load scenario.
- Add a changelog category structure (`compat`, `perf`, `qol`, `render`).

## Suggested Execution Plan

1. **Phase A (1-2 weeks):** quick compatibility/perf knobs + docs + diagnostics.
2. **Phase B (2-4 weeks):** light leak fix, shader target normalization, parser consolidation design.
3. **Phase C (4-8 weeks):** implementation of parser unification, server hardening, async asset pipeline.

## Success Metrics

- Startup crash rate reduction on low-end/older GPUs.
- Lower frame-time variance (95th/99th percentile) on representative maps.
- Reduced memory growth over 30+ minute sessions.
- Faster issue triage due to better startup diagnostics and standardized launch knobs.
