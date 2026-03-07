package com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal quest implementation that tracks quest state for scripting purposes.
 * Full UI quest display is not yet implemented; quest state is stored so that
 * IsQuestCompleted/IsQuestFailed queries return correct values in trigger scripts.
 */
public class CQuest {
	private String title = "";
	private String description = "";
	private String iconPath = "";
	private boolean enabled = true;
	private boolean required = false;
	private boolean completed = false;
	private boolean failed = false;
	private boolean discovered = false;
	private final List<CQuestItem> items = new ArrayList<>();

	public String getTitle() {
		return this.title;
	}

	public void setTitle(final String title) {
		this.title = title != null ? title : "";
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(final String description) {
		this.description = description != null ? description : "";
	}

	public String getIconPath() {
		return this.iconPath;
	}

	public void setIconPath(final String iconPath) {
		this.iconPath = iconPath != null ? iconPath : "";
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(final boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isRequired() {
		return this.required;
	}

	public void setRequired(final boolean required) {
		this.required = required;
	}

	public boolean isCompleted() {
		return this.completed;
	}

	public void setCompleted(final boolean completed) {
		this.completed = completed;
	}

	public boolean isFailed() {
		return this.failed;
	}

	public void setFailed(final boolean failed) {
		this.failed = failed;
	}

	public boolean isDiscovered() {
		return this.discovered;
	}

	public void setDiscovered(final boolean discovered) {
		this.discovered = discovered;
	}

	public CQuestItem createItem(final String description) {
		final CQuestItem item = new CQuestItem(description);
		this.items.add(item);
		return item;
	}

	public List<CQuestItem> getItems() {
		return this.items;
	}
}
