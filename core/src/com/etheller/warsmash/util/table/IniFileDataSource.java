package com.etheller.warsmash.util.table;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.etheller.warsmash.util.IniFile;

/**
 * {@link TableDataSource} adapter for the legacy {@link IniFile} parser.
 */
public final class IniFileDataSource implements TableDataSource {
	private final Set<String> rowKeys = new LinkedHashSet<>();
	private final Set<String> columnKeys = new LinkedHashSet<>();
	private final Map<String, String> rowLookup = new HashMap<>();
	private final Map<String, String> columnLookup = new HashMap<>();
	private final Map<String, Map<String, String>> valuesByRow = new HashMap<>();

	public IniFileDataSource(final IniFile iniFile) {
		if (iniFile != null) {
			index(iniFile);
		}
	}

	private static String normalize(final String key) {
		return key == null ? null : key.toLowerCase();
	}

	private String registerRow(final String rowKey) {
		final String normalizedRow = normalize(rowKey);
		String canonicalRow = this.rowLookup.get(normalizedRow);
		if (canonicalRow == null) {
			canonicalRow = rowKey;
			this.rowLookup.put(normalizedRow, canonicalRow);
			this.rowKeys.add(canonicalRow);
		}
		return canonicalRow;
	}

	private String registerColumn(final String columnKey) {
		final String normalizedColumn = normalize(columnKey);
		String canonicalColumn = this.columnLookup.get(normalizedColumn);
		if (canonicalColumn == null) {
			canonicalColumn = columnKey;
			this.columnLookup.put(normalizedColumn, canonicalColumn);
			this.columnKeys.add(canonicalColumn);
		}
		return canonicalColumn;
	}

	private void index(final IniFile iniFile) {
		for (final Map.Entry<String, Map<String, String>> sectionEntry : iniFile.sections.entrySet()) {
			final String canonicalRow = registerRow(sectionEntry.getKey());
			final Map<String, String> sourceRow = sectionEntry.getValue();
			final Map<String, String> mappedRow = this.valuesByRow.computeIfAbsent(canonicalRow, key -> new HashMap<>());
			for (final Map.Entry<String, String> fieldEntry : sourceRow.entrySet()) {
				final String canonicalColumn = registerColumn(fieldEntry.getKey());
				mappedRow.put(normalize(canonicalColumn), fieldEntry.getValue());
			}
		}
	}

	@Override
	public Set<String> rowKeys() {
		return Collections.unmodifiableSet(this.rowKeys);
	}

	@Override
	public Set<String> columnKeys() {
		return Collections.unmodifiableSet(this.columnKeys);
	}

	@Override
	public Set<String> columnKeys(final String row) {
		if (row == null) {
			return Collections.emptySet();
		}
		final String canonicalRow = this.rowLookup.get(normalize(row));
		if (canonicalRow == null) {
			return Collections.emptySet();
		}
		final Map<String, String> rowData = this.valuesByRow.get(canonicalRow);
		if (rowData == null) {
			return Collections.emptySet();
		}
		final Set<String> rowColumns = new LinkedHashSet<>();
		for (final String normalizedColumn : rowData.keySet()) {
			final String canonicalColumn = this.columnLookup.get(normalizedColumn);
			if (canonicalColumn != null) {
				rowColumns.add(canonicalColumn);
			}
		}
		return Collections.unmodifiableSet(rowColumns);
	}

	@Override
	public String get(final String row, final String col) {
		if ((row == null) || (col == null)) {
			return null;
		}
		final String canonicalRow = this.rowLookup.get(normalize(row));
		final String canonicalColumn = this.columnLookup.get(normalize(col));
		if ((canonicalRow == null) || (canonicalColumn == null)) {
			return null;
		}
		final Map<String, String> rowData = this.valuesByRow.get(canonicalRow);
		if (rowData == null) {
			return null;
		}
		return rowData.get(normalize(canonicalColumn));
	}
}
