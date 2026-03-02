package com.etheller.warsmash.util.table;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.etheller.warsmash.util.SlkFile;

/**
 * {@link TableDataSource} adapter for the legacy {@link SlkFile} parser.
 */
public final class SlkFileDataSource implements TableDataSource {
	private final Set<String> rowKeys = new LinkedHashSet<>();
	private final Set<String> columnKeys = new LinkedHashSet<>();
	private final Map<String, String> rowLookup = new HashMap<>();
	private final Map<String, String> columnLookup = new HashMap<>();
	private final Map<String, Map<String, Object>> valuesByRow = new HashMap<>();

	public SlkFileDataSource(final SlkFile slkFile) {
		if (slkFile != null) {
			index(slkFile);
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

	private void index(final SlkFile slkFile) {
		final List<List<Object>> rows = slkFile.rows;
		if (rows.isEmpty()) {
			return;
		}
		final List<Object> header = rows.get(0);
		if ((header == null) || header.isEmpty()) {
			return;
		}
		int keyColumn = 0;
		for (int i = 1; i < header.size(); i++) {
			final Object headerColumnName = header.get(i);
			if ("AnimationEventCode".equals(headerColumnName)) {
				keyColumn = i;
				break;
			}
		}
		final String[] columnNames = new String[header.size()];
		for (int i = 0; i < header.size(); i++) {
			final Object headerColumn = header.get(i);
			final String columnName = headerColumn == null ? "column" + i : headerColumn.toString();
			columnNames[i] = registerColumn(columnName);
		}

		for (int i = 1; i < rows.size(); i++) {
			final List<Object> row = rows.get(i);
			if (row == null) {
				continue;
			}
			final Object rowNameValue = keyColumn < row.size() ? row.get(keyColumn) : null;
			if (rowNameValue == null) {
				continue;
			}
			final String rowName = rowNameValue.toString();
			final String canonicalRowName = registerRow(rowName);
			final Map<String, Object> mappedRow = this.valuesByRow.computeIfAbsent(canonicalRowName,
					key -> new HashMap<>());

			for (int columnIndex = 0; columnIndex < columnNames.length; columnIndex++) {
				final Object value = columnIndex < row.size() ? row.get(columnIndex) : null;
				mappedRow.put(normalize(columnNames[columnIndex]), value);
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
	public String get(final String row, final String col) {
		final Object value = getRaw(row, col);
		return value == null ? null : value.toString();
	}

	@Override
	public Object getRaw(final String row, final String col) {
		if ((row == null) || (col == null)) {
			return null;
		}
		final String canonicalRow = this.rowLookup.get(normalize(row));
		final String canonicalColumn = this.columnLookup.get(normalize(col));
		if ((canonicalRow == null) || (canonicalColumn == null)) {
			return null;
		}
		final Map<String, Object> rowData = this.valuesByRow.get(canonicalRow);
		if (rowData == null) {
			return null;
		}
		return rowData.get(normalize(canonicalColumn));
	}
}
