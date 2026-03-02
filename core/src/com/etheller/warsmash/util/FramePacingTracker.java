package com.etheller.warsmash.util;

/**
 * Lightweight frame-pacing tracker.
 *
 * Call {@link #tick(float)} once per rendered frame with the raw delta time in
 * seconds. Every {@value #REPORT_INTERVAL_SECONDS} seconds a one-line summary
 * of min/max/average frame time and effective FPS is printed to stdout.
 */
public final class FramePacingTracker {

	private static final int REPORT_INTERVAL_SECONDS = 60;
	/** Ring-buffer capacity — roughly 10 s of history at 60 fps. */
	private static final int WINDOW_FRAMES = 600;

	private final float[] frameTimes = new float[WINDOW_FRAMES];
	private int head = 0;
	private int count = 0;
	private float elapsedSinceReport = 0f;

	public void tick(final float deltaSeconds) {
		if (deltaSeconds <= 0f) {
			return;
		}
		frameTimes[head] = deltaSeconds * 1000f;
		head = (head + 1) % WINDOW_FRAMES;
		if (count < WINDOW_FRAMES) {
			count++;
		}

		elapsedSinceReport += deltaSeconds;
		if (elapsedSinceReport >= REPORT_INTERVAL_SECONDS) {
			report();
			elapsedSinceReport = 0f;
		}
	}

	private void report() {
		if (count == 0) {
			return;
		}
		float min = Float.MAX_VALUE;
		float max = -Float.MAX_VALUE;
		float sum = 0f;
		for (int i = 0; i < count; i++) {
			final float t = frameTimes[i];
			if (t < min) {
				min = t;
			}
			if (t > max) {
				max = t;
			}
			sum += t;
		}
		final float avg = sum / count;
		System.out.printf("[FramePacing] avg=%.2f ms  min=%.2f ms  max=%.2f ms  fps=%.1f  (window: %d frames)%n",
				avg, min, max, 1000f / avg, count);
	}
}
