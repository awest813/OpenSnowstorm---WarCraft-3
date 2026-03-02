package com.etheller.warsmash.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.etheller.warsmash.util.table.IniFileDataSource;
import com.etheller.warsmash.util.table.SlkFileDataSource;
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
		if (buffer.startsWith("ID;")) {
			load(new SlkFileDataSource(new SlkFile(buffer)));
		}
		else {
			load(new IniFileDataSource(new IniFile(buffer)));
		}
	}

	/**
	 * Load data from any table-backed data source.
	 *
	 * Note that this may override previous properties!
	 */
	public void load(final TableDataSource dataSource) {
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
				mapped.put(column, dataSource.getRaw(row, column));
			}
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
