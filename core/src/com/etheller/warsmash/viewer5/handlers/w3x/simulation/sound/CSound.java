package com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound;

public interface CSound {
	void start();

	void stop();

	float getPredictedDuration();

	float getRemainingTimeToPlayOnTheDesyncLocalComputer();

	void setVolume(int volume);

	void setPitch(float pitch);

	void setPosition(float x, float y, float z);

	boolean isPlaying();
}
