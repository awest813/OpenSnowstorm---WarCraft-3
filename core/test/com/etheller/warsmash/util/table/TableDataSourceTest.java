package com.etheller.warsmash.util.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.etheller.warsmash.units.DataTable;
import com.etheller.warsmash.util.IniFile;
import com.etheller.warsmash.util.MappedData;
import com.etheller.warsmash.util.SlkFile;
import com.etheller.warsmash.util.StringBundle;

class TableDataSourceTest {
	private static final String SAMPLE_SLK = String.join("\n", "ID;PWXL;N;E", "B;Y3;X3;D0", "C;Y1;X1;K\"ID\"",
			"C;X2;K\"Name\"", "C;X3;K\"Scale\"", "C;Y2;X1;K\"A001\"", "C;X2;K\"Footman\"", "C;X3;K1.5",
			"C;Y3;X1;K\"A002\"", "C;X2;K\"Knight\"", "C;X3;K2", "E", "");

	private static final String SAMPLE_INI = String.join("\n", "// sample", "[Footman]", "Name=Footman", "HP=420",
			"", "[Knight]", "Name=Knight", "HP=800", "");
	private static final String SAMPLE_TYPED_SLK = String.join("\n", "ID;PWXL;N;E", "B;Y2;X3;D0", "C;Y1;X1;K\"ID\"",
			"C;X2;K\"Enabled\"", "C;X3;K\"Speed\"", "C;Y2;X1;K\"A001\"", "C;X2;KTRUE", "C;X3;K270",
			"E", "");

	private static Set<String> lowerCaseSet(final Set<String> input) {
		final Set<String> output = new HashSet<>();
		for (final String value : input) {
			output.add(value.toLowerCase());
		}
		return output;
	}

	private static void assertParity(final TableDataSource legacySource, final TableDataSource dataTableSource) {
		final Set<String> rows = lowerCaseSet(legacySource.rowKeys());
		assertEquals(rows, lowerCaseSet(dataTableSource.rowKeys()), "Row-key mismatch");

		for (final String row : rows) {
			final Set<String> columns = lowerCaseSet(legacySource.columnKeys(row));
			columns.addAll(lowerCaseSet(dataTableSource.columnKeys(row)));
			for (final String column : columns) {
				final String legacyValue = legacySource.get(row, column);
				final String dataTableValue = dataTableSource.get(row, column);
				if ((legacyValue != null) && (dataTableValue != null)) {
					try {
						assertEquals(Double.parseDouble(legacyValue), Double.parseDouble(dataTableValue), 0.000001d,
								"Cell mismatch at row=" + row + ", column=" + column);
					}
					catch (final NumberFormatException ignored) {
						assertEquals(legacyValue, dataTableValue,
								"Cell mismatch at row=" + row + ", column=" + column);
					}
				}
				else {
					assertEquals(legacyValue, dataTableValue, "Cell mismatch at row=" + row + ", column=" + column);
				}
			}
		}
	}

	@Test
	void slkLegacyAndDataTableAdaptersMatch() throws IOException {
		final TableDataSource legacySource = new SlkFileDataSource(new SlkFile(SAMPLE_SLK));

		final DataTable dataTable = new DataTable(StringBundle.EMPTY);
		dataTable.readSLK(new ByteArrayInputStream(SAMPLE_SLK.getBytes(StandardCharsets.UTF_8)));
		final TableDataSource dataTableSource = new DataTableSource(dataTable);

		assertParity(legacySource, dataTableSource);
	}

	@Test
	void iniLegacyAndDataTableAdaptersMatch() throws IOException {
		final TableDataSource legacySource = new IniFileDataSource(new IniFile(SAMPLE_INI));

		final DataTable dataTable = new DataTable(StringBundle.EMPTY);
		dataTable.readTXT(new ByteArrayInputStream(SAMPLE_INI.getBytes(StandardCharsets.UTF_8)), true);
		final TableDataSource dataTableSource = new DataTableSource(dataTable);

		assertParity(legacySource, dataTableSource);
	}

	@Test
	void slkLookupsRemainTypedInMappedData() {
		final MappedData mappedData = new MappedData(SAMPLE_SLK);
		final Object scale = mappedData.getProperty("A001", "Scale");

		assertInstanceOf(Number.class, scale);
		assertEquals(1.5f, ((Number) scale).floatValue(), 0.0001f);
		assertEquals("Footman", mappedData.getProperty("A001", "Name"));
		assertNull(mappedData.getProperty("A001", "MissingColumn"));
	}

	@Test
	void slkBooleansAndIntegersRemainTypedInMappedData() {
		final MappedData mappedData = new MappedData(SAMPLE_TYPED_SLK);
		assertInstanceOf(Boolean.class, mappedData.getProperty("A001", "Enabled"));
		assertEquals(Boolean.TRUE, mappedData.getProperty("A001", "Enabled"));
		assertInstanceOf(Number.class, mappedData.getProperty("A001", "Speed"));
		assertEquals(270f, ((Number) mappedData.getProperty("A001", "Speed")).floatValue(), 0.0001f);
	}
}
