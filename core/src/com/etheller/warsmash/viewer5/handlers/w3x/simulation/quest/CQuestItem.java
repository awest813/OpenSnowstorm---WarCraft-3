package com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest;

/**
 * A single trackable objective within a {@link CQuest}.
 */
public class CQuestItem {
	private String description;
	private boolean completed = false;

	public CQuestItem(final String description) {
		this.description = description != null ? description : "";
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(final String description) {
		this.description = description != null ? description : "";
	}

	public boolean isCompleted() {
		return this.completed;
	}

	public void setCompleted(final boolean completed) {
		this.completed = completed;
	}
}
