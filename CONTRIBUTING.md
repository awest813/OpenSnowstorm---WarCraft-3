# Contributing to Warsmash

Thank you for your interest in contributing. This guide covers the development
workflow, coding conventions, and profiling approach used in the project.

---

## Table of Contents

1. [Getting Started](#getting-started)
2. [Coding Conventions](#coding-conventions)
3. [Submitting Changes](#submitting-changes)
4. [Profiling Workflow](#profiling-workflow)
5. [Changelog Categories](#changelog-categories)

---

## Getting Started

### Prerequisites

- **Java 17** — [Eclipse Temurin 17](https://adoptium.net/) is strongly
  recommended (the Ubuntu OpenJDK 17 apt package has known native-library issues
  with LibGDX).
- **Gradle** — use the included `./gradlew` wrapper; do not install Gradle
  system-wide.
- **Warcraft III assets** — Warsmash requires a legally-purchased copy of
  Warcraft III. See the [README](README.md) for INI configuration.

### Building

```bash
./gradlew assemble          # compile all subprojects
./gradlew desktop:runGame   # run the game (requires warsmash.ini + WC3 assets)
```

Pass extra flags with `-Pargs`:

```bash
./gradlew desktop:runGame -Pargs="-window 1280 720 -fps 60 -nolog"
```

Validate your asset paths before launching:

```bash
./gradlew desktop:runGame -Pargs="-validate"
```

---

## Coding Conventions

The codebase currently uses **Java 8 syntax** running on Java 17. Please keep
new code within that constraint until a deliberate migration is planned.

### Style

- **Indentation:** tabs (matching existing code).
- **Braces:** opening brace on the same line as the declaration/statement.
- **Naming:** follow standard Java naming (`camelCase` for methods/fields,
  `PascalCase` for classes, `UPPER_SNAKE_CASE` for constants).
- **Imports:** no wildcard imports; keep them grouped (standard library, then
  third-party, then project-internal).

### Comments

- Prefer self-documenting code over comments that narrate what the code does.
- Use Javadoc on public API methods and non-obvious class contracts.
- Use inline comments only to explain *why*, not *what*.

### Architecture Boundaries

The project has four primary layers. Keep cross-layer dependencies minimal:

| Layer | Subproject | Purpose |
|---|---|---|
| `render` | `core` (viewer5) | MDX/W3X model loading and OpenGL rendering |
| `simulation` | `core` (simulation) | Game logic, units, abilities, JASS VM |
| `assets` | `core` (datasources) | Virtual file system (MPQ / CASC / folder) |
| `net` | `server`, `shared` | Multiplayer lobby and session protocol |

---

## Submitting Changes

1. Fork the repository and create a feature branch from `main`.
2. Keep commits small and focused; write a descriptive commit message.
3. Add your change to `CHANGELOG.md` under the appropriate category and the
   `[Unreleased]` section.
4. Open a pull request against `main`. The CI build must pass.

---

## Profiling Workflow

### Built-in Frame Pacing Log

Warsmash logs a frame-pacing summary to stdout every 60 seconds:

```
[FramePacing] avg=16.67 ms  min=15.20 ms  max=22.40 ms  fps=59.9  (window: 600 frames)
```

A high `max` relative to `avg` indicates frame spikes. Collect several lines
before and after a suspected regression to compare.

### VisualVM / JProfiler

1. Launch with the Gradle `debug` task to enable JVM attach:
   ```bash
   ./gradlew desktop:debug
   ```
2. Attach VisualVM or JProfiler to the running JVM process.
3. Enable CPU sampling on the `com.etheller.warsmash` package tree.
4. Load a representative test map and let it run for at least 2 minutes before
   capturing a snapshot.

### Hotspot Candidates

Based on the roadmap analysis, focus profiling on:

- `viewer5/handlers/w3x/simulation/` — game simulation tick (called every
  frame; avoid per-frame heap allocations).
- `datasources/` — asset cache hit rate (use log output to identify repeated
  loads of the same file).
- Light system (`viewer5/`) — known to leak instances; profile with a long
  session and track `Light` object count in the heap.

### Reproducing a Performance Issue

Use a fixed-size windowed session with a capped frame rate to get reproducible
numbers:

```bash
./gradlew desktop:runGame -Pargs="-window 1280 720 -fps 60 -nolog -loadfile YourMap.w3x"
```

Capture frame-pacing log output from the console for before/after comparison.

---

## Changelog Categories

When adding an entry to `CHANGELOG.md`, use one of these prefixes:

| Category | Description |
|---|---|
| `compat` | Hardware/driver/OS compatibility fix or improvement |
| `perf` | Performance or memory improvement |
| `qol` | Quality-of-life improvement for players or developers |
| `render` | Rendering correctness or visual improvement |
| `fix` | General bug fix that does not fit another category |
| `break` | Breaking change that requires user action |
