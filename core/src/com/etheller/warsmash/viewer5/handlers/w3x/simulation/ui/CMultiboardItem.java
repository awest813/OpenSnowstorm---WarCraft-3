package com.etheller.warsmash.viewer5.handlers.w3x.simulation.ui;

/**
 * A single cell inside a {@link CMultiboard}. Only state is stored; rendering
 * is not yet implemented.
 */
public class CMultiboardItem {
	private String value = "";
	private String icon = "";
	private float valueColor_r = 1.0f;
	private float valueColor_g = 1.0f;
	private float valueColor_b = 1.0f;
	private float valueColor_a = 1.0f;
	private int style = 0;
	private float width = 0.0f;

	public String getValue() {
		return this.value;
	}

	public void setValue(final String value) {
		this.value = value != null ? value : "";
	}

	public String getIcon() {
		return this.icon;
	}

	public void setIcon(final String icon) {
		this.icon = icon != null ? icon : "";
	}

	public void setValueColor(final float r, final float g, final float b, final float a) {
		this.valueColor_r = r;
		this.valueColor_g = g;
		this.valueColor_b = b;
		this.valueColor_a = a;
	}

	public int getStyle() {
		return this.style;
	}

	public void setStyle(final int style) {
		this.style = style;
	}

	public float getWidth() {
		return this.width;
	}

	public void setWidth(final float width) {
		this.width = width;
	}
}
