package com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.TimeUtils;
import com.etheller.warsmash.viewer5.AudioBufferSource;
import com.etheller.warsmash.viewer5.AudioContext;
import com.etheller.warsmash.viewer5.AudioPanner;
import com.etheller.warsmash.viewer5.gl.Extensions;

public class CSoundFilename implements CSound {
	private final Sound sound;
	private final boolean looping;
	private final boolean stopWhenOutOfRange;
	private final int fadeInRate;
	private final int fadeOutRate;
	private final AudioContext audioContext;
	private float x;
	private float y;
	private float z;
	private float volume = 1.0f;
	private float pitch = 1.0f;
	private final float minDistance = 99999;
	private float distanceCutoff = 99999;
	private final String eaxSetting;
	private long lastStartTimestamp;
	private long lastSoundInstanceId = -1;
	private boolean playing = false;

	public CSoundFilename(final Sound sound, final AudioContext audioContext, final boolean looping,
			final boolean stopWhenOutOfRange, final int fadeInRate, final int fadeOutRate, final String eaxSetting) {
		this.sound = sound;
		this.audioContext = audioContext;
		this.looping = looping;
		this.stopWhenOutOfRange = stopWhenOutOfRange;
		this.fadeInRate = fadeInRate;
		this.fadeOutRate = fadeOutRate;
		this.eaxSetting = eaxSetting;
	}

	@Override
	public void start() {
		if (this.audioContext == null) {
			return;
		}
		final AudioPanner panner = this.audioContext.createPanner(this.stopWhenOutOfRange);
		final AudioBufferSource source = this.audioContext.createBufferSource();

		// Panner settings
		panner.setPosition(this.x, this.y, this.z);
		panner.setDistances(this.distanceCutoff, this.minDistance);
		panner.connect(this.audioContext.destination);

		// Source.
		source.buffer = this.sound;
		source.connect(panner);

		// Make a sound.
		this.lastSoundInstanceId = source.start(0, this.volume, this.pitch, this.looping);
		this.playing = true;

		this.lastStartTimestamp = TimeUtils.millis();
	}

	@Override
	public void stop() {
		if (this.sound != null) {
			if (this.lastSoundInstanceId != -1) {
				this.sound.stop(this.lastSoundInstanceId);
			}
			else {
				this.sound.stop();
			}
		}
		this.playing = false;
		this.lastSoundInstanceId = -1;
	}

	@Override
	public void setVolume(final int volume) {
		// WC3 uses 0-127 range; normalize to 0.0-1.0
		this.volume = Math.max(0, Math.min(127, volume)) / 127.0f;
	}

	@Override
	public void setPitch(final float pitch) {
		this.pitch = pitch;
	}

	@Override
	public void setPosition(final float x, final float y, final float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public void setDistanceCutoff(final float cutoff) {
		this.distanceCutoff = cutoff;
	}

	@Override
	public boolean isPlaying() {
		if (!this.playing) {
			return false;
		}
		final long currentTime = TimeUtils.millis();
		final long deltaTime = currentTime - this.lastStartTimestamp;
		final float deltaTimeSeconds = deltaTime / 1000f;
		if (!this.looping && (deltaTimeSeconds >= getPredictedDuration())) {
			this.playing = false;
			return false;
		}
		return true;
	}

	@Override
	public float getPredictedDuration() {
		return Extensions.audio.getDuration(this.sound);
	}

	@Override
	public float getRemainingTimeToPlayOnTheDesyncLocalComputer() {
		final long currentTime = TimeUtils.millis();
		final long deltaTime = currentTime - this.lastStartTimestamp;
		final float deltaTimeSeconds = deltaTime / 1000f;
		final float predictedDuration = getPredictedDuration();
		if (deltaTimeSeconds > predictedDuration) {
			return 0;
		}
		return predictedDuration - deltaTimeSeconds;
	}

}
