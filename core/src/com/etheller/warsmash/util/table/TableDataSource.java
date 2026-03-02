package com.etheller.warsmash.util.table;

import java.util.Set;

/**
 * Read-only tabular data abstraction for SLK/INI/DataTable sources.
 */
public interface TableDataSource {
	/**
	 * Returns all row keys known by this source.
	 */
	Set<String> rowKeys();

	/**
	 * Returns all column keys known by this source.
	 */
	Set<String> columnKeys();

	/**
	 * Returns column keys for a specific row. Defaults to global column keys.
	 */
	default Set<String> columnKeys(final String row) {
		return columnKeys();
	}

	/**
	 * Returns a case-insensitive string lookup for a cell, or {@code null}.
	 */
	String get(String row, String col);

	/**
	 * Returns the raw (possibly typed) value for a cell, or {@code null}.
	 */
	default Object getRaw(final String row, final String col) {
		return get(row, col);
	}
}
