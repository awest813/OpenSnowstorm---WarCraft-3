package com.etheller.warsmash.util.table;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.etheller.warsmash.units.DataTable;
import com.etheller.warsmash.units.Element;

/**
 * {@link TableDataSource} adapter for {@link DataTable}.
 */
public final class DataTableSource implements TableDataSource {
	private final DataTable table;
	private final Set<String> rowKeys = new LinkedHashSet<>();
	private final Set<String> columnKeys = new LinkedHashSet<>();
	private final Map<String, String> rowLookup = new HashMap<>();
	private final Map<String, String> columnLookup = new HashMap<>();

	public DataTableSource(final DataTable table) {
		this.table = table;
		if (table != null) {
			index(table);
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

	private void index(final DataTable sourceTable) {
		for (final String row : sourceTable.keySet()) {
			registerRow(row);
		}
		for (final String row : this.rowKeys) {
			final Element element = sourceTable.get(row);
			if (element == null) {
				continue;
			}
			for (final String field : element.keySet()) {
				registerColumn(field);
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
		if ((this.table == null) || (row == null)) {
			return Collections.emptySet();
		}
		final String canonicalRow = this.rowLookup.get(normalize(row));
		if (canonicalRow == null) {
			return Collections.emptySet();
		}
		final Element element = this.table.get(canonicalRow);
		if (element == null) {
			return Collections.emptySet();
		}
		final Set<String> rowColumns = new LinkedHashSet<>();
		for (final String field : element.keySet()) {
			final String canonicalColumn = this.columnLookup.get(normalize(field));
			rowColumns.add(canonicalColumn == null ? field : canonicalColumn);
		}
		return Collections.unmodifiableSet(rowColumns);
	}

	@Override
	public String get(final String row, final String col) {
		if ((this.table == null) || (row == null) || (col == null)) {
			return null;
		}
		final String canonicalRow = this.rowLookup.get(normalize(row));
		if (canonicalRow == null) {
			return null;
		}
		final Element element = this.table.get(canonicalRow);
		if (element == null) {
			return null;
		}
		String canonicalColumn = this.columnLookup.get(normalize(col));
		if (canonicalColumn == null) {
			canonicalColumn = col;
		}
		if (element.hasField(canonicalColumn)) {
			return element.getField(canonicalColumn);
		}
		// In SLK-backed tables the row identifier is effectively the logical "ID" field.
		if ("id".equalsIgnoreCase(canonicalColumn) || "animationeventcode".equalsIgnoreCase(canonicalColumn)) {
			return canonicalRow;
		}
		return null;
	}
}
