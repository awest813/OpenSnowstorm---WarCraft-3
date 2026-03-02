# Parser Consolidation Design

> **Phase B deliverable — design only; implementation is Phase C.**

## Problem Statement

The codebase maintains two separate parser stacks for the same two data
formats (SLK and INI/TXT):

| Format | Viewer parser | ReteraModelStudio parser |
|--------|---------------|--------------------------|
| SLK | `core/…/util/SlkFile.java` | `core/…/units/DataTable.readSLK()` |
| INI/TXT | `core/…/util/IniFile.java` | `core/…/units/DataTable.readTXT()` |

The viewer parsers feed into `MappedData` and handle splat data, anim sounds,
and other viewer-level tables. The `DataTable` parsers produce `Element` /
`LMUnit` objects and power the high-level unit-data, ability, and terrain APIs.

Maintaining two separate parsers for each format creates:
- Divergent bug surfaces (a fix in one parser does not carry over to the other).
- Double test burden.
- Inconsistent field semantics across the two layers.

---

## 1. Unified Interface

Introduce a single read-only interface `TableDataSource` in a new package
`com.etheller.warsmash.datasource` (or under `util`):

```java
public interface TableDataSource {
    /** Returns all row keys present in the table. */
    Set<String> rowKeys();

    /** Returns all column keys present in the table. */
    Set<String> columnKeys();

    /**
     * Returns the raw string value at (row, col), or {@code null} if absent.
     * Row and column keys are case-insensitive.
     */
    String get(String row, String col);
}
```

Both the SLK and INI code paths will expose their data through this interface.
Callers that currently accept `SlkFile` or `IniFile` directly will be migrated
to accept `TableDataSource`.

---

## 2. Recommended Backend

**Keep the `DataTable` backend** (`DataTable.readSLK` / `DataTable.readTXT`) as
the canonical implementation, for the following reasons:

1. It is richer — it understands field inheritance and already powers the
   production unit-data, ability, and terrain APIs.
2. It is exercised by more call sites and therefore better tested in practice.
3. The `Element` / `LMUnit` model is already the authoritative representation
   for game data.

The viewer parsers (`SlkFile`, `IniFile`) will eventually become thin adapters
or be removed entirely once all callers have been migrated.

---

## 3. Adapter Layer

To allow incremental migration without a big-bang rewrite, introduce two
adapters:

### 3.1 `DataTableSource`

Wraps an existing `DataTable` instance and implements `TableDataSource`:

```java
public final class DataTableSource implements TableDataSource {
    private final DataTable table;
    public DataTableSource(DataTable table) { this.table = table; }

    @Override public Set<String> rowKeys() { /* delegate */ }
    @Override public Set<String> columnKeys() { /* delegate */ }
    @Override public String get(String row, String col) { /* delegate */ }
}
```

### 3.2 `SlkFileSource` / `IniFileSource`

Wrap the legacy viewer parsers and implement `TableDataSource`. These adapters
let existing `MappedData` callers switch to the interface *before* their
underlying implementation is swapped:

```java
public final class SlkFileSource implements TableDataSource {
    private final SlkFile slk;
    public SlkFileSource(SlkFile slk) { this.slk = slk; }
    // ... delegate to SlkFile
}
```

---

## 4. Migration Order

Migrate callers from lowest risk to highest risk:

| Priority | Caller | Risk | Notes |
|----------|--------|------|-------|
| 1 | Splat / ground-texture SLK readers | Low | Read-only; limited column set |
| 2 | Anim-sound SLK / INI readers | Low | Read-only; limited column set |
| 3 | `MappedData` general table loading | Medium | Wider call surface |
| 4 | Unit-data / ability readers (currently `DataTable`) | Low — already on canonical backend | Confirm interface compatibility |
| 5 | Terrain / cliffs / doodad readers | High | Dense cross-references; test carefully |

---

## 5. Test Strategy

Before removing any legacy parser, add a **parity test** that:

1. Parses the same reference SLK/INI file with both the old path (legacy parser)
   and the new path (`DataTable`-backed `TableDataSource`).
2. Asserts that `rowKeys()`, `columnKeys()`, and all `get(r, c)` calls return
   identical values.

Reference files to use:
- `Units/UnitData.slk` — representative SLK with inheritance.
- `Units/UnitMetaData.slk` — wider column set.
- `UI/SoundInfo/AnimLookups.slk` — viewer-level SLK.
- `war3map.w3u` parsed into INI — representative INI.

Parity tests should be placed in a new `test` source set and run as part of the
existing CI matrix (Ubuntu + Windows × Java 17 + 21).

---

## 6. Open Questions

1. **Field inheritance depth**: `DataTable` resolves parent-chain inheritance
   (`Slk`, `Profile`, `Campaign` suffixes). The viewer `SlkFile` does not.
   Callers relying on un-resolved values will see different data after
   migration. Audit all call sites before migration.

2. **Comment handling**: `IniFile` strips `//` comments; `DataTable.readTXT`
   behaviour should be verified to match.

3. **Package location**: `TableDataSource` could live in `util` (alongside the
   existing parsers) or in a new `datasource` package. Decide before
   implementation begins to avoid a rename refactor later.

4. **Streaming vs. in-memory**: Both current parsers are fully in-memory.
   Phase C can defer a streaming/lazy-loading redesign to a later phase if
   memory is not a bottleneck.

---

*This document is a design artefact for Phase B. The actual implementation is
scheduled for Phase C. See `docs/ENGINE_MODERNIZATION_ANALYSIS.md` for the
overall roadmap.*
