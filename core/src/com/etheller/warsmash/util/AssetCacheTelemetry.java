package com.etheller.warsmash.util;

/**
 * Lightweight hit/miss telemetry for the {@code ModelViewer} fetch cache.
 *
 * <p>Tracks every asset lookup that passes through the two cache paths in
 * {@code ModelViewer}: {@code load()} and {@code loadGeneric()}. A periodic
 * summary is printed to stdout every {@value #REPORT_INTERVAL} misses so the
 * cache effectiveness can be evaluated without a profiler.
 *
 * <p>Usage:
 * <pre>
 *   // cache hit path
 *   if (cached != null) {
 *       AssetCacheTelemetry.recordHit(path);
 *       return cached;
 *   }
 *   // cache miss path
 *   AssetCacheTelemetry.recordMiss(path);
 * </pre>
 *
 * <p>This class is not thread-safe; all calls must occur on the LibGDX
 * render thread.
 */
public final class AssetCacheTelemetry {

	/** Print a summary after every N cache misses. */
	private static final int REPORT_INTERVAL = 50;

	private static long totalHits = 0;
	private static long totalMisses = 0;
	private static long missCountSinceLastReport = 0;

	private AssetCacheTelemetry() {
	}

	/**
	 * Records a cache hit for {@code assetPath}. The path is used only for
	 * potential future per-asset breakdown; it is not stored beyond this call.
	 *
	 * @param assetPath the asset path that was found in the cache
	 */
	public static void recordHit(final String assetPath) {
		totalHits++;
	}

	/**
	 * Records a cache miss for {@code assetPath} and triggers a periodic
	 * report when {@value #REPORT_INTERVAL} misses have accumulated.
	 *
	 * @param assetPath the asset path that was not found in the cache
	 */
	public static void recordMiss(final String assetPath) {
		totalMisses++;
		missCountSinceLastReport++;
		if (missCountSinceLastReport >= REPORT_INTERVAL) {
			report();
			missCountSinceLastReport = 0;
		}
	}

	/**
	 * Returns the total number of cache hits since the JVM started (or since
	 * the last {@link #reset()} call).
	 */
	public static long getTotalHits() {
		return totalHits;
	}

	/**
	 * Returns the total number of cache misses since the JVM started (or since
	 * the last {@link #reset()} call).
	 */
	public static long getTotalMisses() {
		return totalMisses;
	}

	/**
	 * Returns the cache hit rate as a value between 0.0 and 1.0, or 0.0 when
	 * no lookups have been recorded.
	 */
	public static double hitRate() {
		final long total = totalHits + totalMisses;
		return (total == 0) ? 0.0 : (double) totalHits / total;
	}

	/** Resets all counters. */
	public static void reset() {
		totalHits = 0;
		totalMisses = 0;
		missCountSinceLastReport = 0;
	}

	/** Emits the current hit/miss summary to stdout. */
	public static void report() {
		final long total = totalHits + totalMisses;
		final double rate = (total == 0) ? 0.0 : 100.0 * totalHits / total;
		System.out.printf(
				"[AssetCache] hits=%d  misses=%d  total=%d  hitRate=%.1f%%%n",
				totalHits, totalMisses, total, rate);
	}
}
