package com.etheller.warsmash.util;

import java.util.Arrays;

/**
 * Lightweight frame-pacing tracker.
 *
 * Call {@link #tick(float)} once per rendered frame with the raw delta time in
 * seconds. Every {@value #REPORT_INTERVAL_SECONDS} seconds a one-line summary
 * of avg/p95/p99/max frame time and effective FPS is printed to stdout.
 *
 * <p>p95 and p99 percentiles flag intermittent frame spikes that would be
 * masked by the average alone. A healthy session shows p99 &lt; 2× avg; a
 * leaky session shows p99 climbing over time.
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
		// Sort a copy so we can compute percentiles without disturbing the ring buffer.
		final float[] sorted = Arrays.copyOf(frameTimes, count);
		Arrays.sort(sorted);

		float sum = 0f;
		for (int i = 0; i < count; i++) {
			sum += sorted[i];
		}
		final float avg = sum / count;
		final float min = sorted[0];
		final float max = sorted[count - 1];

		// Percentile indices — clamp to valid range.
		final float p95 = sorted[Math.min((int) (count * 0.95f), count - 1)];
		final float p99 = sorted[Math.min((int) (count * 0.99f), count - 1)];

		System.out.printf(
				"[FramePacing] avg=%.2f ms  p95=%.2f ms  p99=%.2f ms  max=%.2f ms  fps=%.1f  (window: %d frames)%n",
				avg, p95, p99, max, 1000f / avg, count);

		if (p99 > avg * 3f) {
			System.out.printf("[FramePacing] WARNING: p99 is %.1fx the average — frame spikes detected.%n",
					p99 / avg);
		}
	}
}
