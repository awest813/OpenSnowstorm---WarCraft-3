package com.etheller.warsmash.util;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.nio.charset.StandardCharsets;

import com.etheller.warsmash.units.DataTable;
import com.etheller.warsmash.util.table.DataTableSource;
import com.etheller.warsmash.util.table.TableDataSource;

/**
 * A structure that holds mapped data from INI and SLK files.
 *
 * In the case of SLK files, the first row is expected to hold the names of the
 * columns.
 */
public class MappedData implements Iterable<Map.Entry<String, MappedDataRow>> {
	private final Map<String, MappedDataRow> map = new HashMap<>();

	public MappedData() {
		this(null);
	}

	public MappedData(final String buffer) {
		if (buffer != null) {
			load(buffer);
		}
	}

	/**
	 * Load data from an SLK file or an INI file.
	 *
	 * Note that this may override previous properties!
	 */
	public void load(final String buffer) {
		if (buffer == null) {
			return;
		}
		final boolean slkLike = buffer.startsWith("ID;");
		final DataTable table = new DataTable(StringBundle.EMPTY);
		try (ByteArrayInputStream stream = new ByteArrayInputStream(buffer.getBytes(StandardCharsets.UTF_8))) {
			if (slkLike) {
				table.readSLK(stream);
			}
			else {
				table.readTXT(stream, true);
			}
		}
		catch (final Exception e) {
			throw new RuntimeException("Failed to parse mapped data buffer", e);
		}
		load(new DataTableSource(table), slkLike);
	}

	/**
	 * Load data from any table-backed data source.
	 *
	 * Note that this may override previous properties!
	 */
	public void load(final TableDataSource dataSource) {
		load(dataSource, false);
	}

	private void load(final TableDataSource dataSource, final boolean coerceSlkTypes) {
		if (dataSource == null) {
			return;
		}
		for (final String row : dataSource.rowKeys()) {
			if (row == null) {
				continue;
			}
			final String normalizedRow = row.toLowerCase();
			MappedDataRow mapped = this.map.get(normalizedRow);
			if (mapped == null) {
				mapped = new MappedDataRow();
				this.map.put(normalizedRow, mapped);
			}
			for (final String column : dataSource.columnKeys(row)) {
				if (column == null) {
					continue;
				}
				final Object rawValue = dataSource.getRaw(row, column);
				mapped.put(column, coerceSlkTypes ? coerceSlkValue(rawValue) : rawValue);
			}
		}
	}

	private static Object coerceSlkValue(final Object rawValue) {
		if (!(rawValue instanceof String)) {
			return rawValue;
		}
		final String stringValue = (String) rawValue;
		if ("TRUE".equalsIgnoreCase(stringValue)) {
			return Boolean.TRUE;
		}
		if ("FALSE".equalsIgnoreCase(stringValue)) {
			return Boolean.FALSE;
		}
		try {
			return Float.parseFloat(stringValue);
		}
		catch (final NumberFormatException ignored) {
			return stringValue;
		}
	}

	public MappedDataRow getRow(final String key) {
		return this.map.get(key.toLowerCase());
	}

	public Object getProperty(final String key, final String name) {
		return this.map.get(key.toLowerCase()).get(name);
	}

	public void setRow(final String key, final MappedDataRow values) {
		this.map.put(key.toLowerCase(), values);
	}

	@Override
	public Iterator<Entry<String, MappedDataRow>> iterator() {
		return this.map.entrySet().iterator();
	}
}
