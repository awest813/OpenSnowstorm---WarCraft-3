package com.etheller.warsmash.viewer5.handlers.w3x.simulation.ui;

import java.util.HashMap;
import java.util.Map;

/**
 * Minimal multiboard implementation. Full rendering is not yet implemented;
 * the object exists so that scripts that create and manipulate multiboards do
 * not crash due to null-handle dereferences.
 */
public class CMultiboard {
	private String title = "";
	private int columns = 0;
	private int rows = 0;
	private boolean displayed = false;
	private boolean minimized = false;
	/** Persistent item cells indexed by row * MAX_COLS + col. */
	private final Map<Integer, CMultiboardItem> items = new HashMap<>();

	public String getTitle() {
		return this.title;
	}

	public void setTitle(final String title) {
		this.title = title != null ? title : "";
	}

	public int getColumns() {
		return this.columns;
	}

	public void setColumns(final int columns) {
		this.columns = Math.max(0, columns);
	}

	public int getRows() {
		return this.rows;
	}

	public void setRows(final int rows) {
		this.rows = Math.max(0, rows);
	}

	public boolean isDisplayed() {
		return this.displayed;
	}

	public void setDisplayed(final boolean displayed) {
		this.displayed = displayed;
	}

	public boolean isMinimized() {
		return this.minimized;
	}

	public void setMinimized(final boolean minimized) {
		this.minimized = minimized;
	}

	/**
	 * Returns (or lazily creates) the persistent item cell for the given row and
	 * column. Row and column are 0-based internally; JASS passes 1-based values so
	 * callers must subtract 1 before calling this method if they want 0-based
	 * storage.
	 */
	public CMultiboardItem getItem(final int row, final int column) {
		final int key = row * 1000 + column;
		CMultiboardItem item = this.items.get(key);
		if (item == null) {
			item = new CMultiboardItem();
			this.items.put(key, item);
		}
		return item;
	}
}
