package com.etheller.warsmash.util;

/**
 * Tracks how long the simulation tick takes relative to a configurable frame
 * budget and reports overruns to stdout.
 *
 * <p>Usage:
 * <pre>
 *   SimulationBudgetTracker tracker = new SimulationBudgetTracker();
 *   ...
 *   long t0 = tracker.beginTick();
 *   simulation.step();
 *   tracker.endTick(t0);
 * </pre>
 *
 * <p>A summary line is printed every {@value #REPORT_INTERVAL_TICKS} ticks
 * (~60 s at 60 fps). An additional warning is emitted whenever a single tick
 * exceeds the configured budget.
 */
public final class SimulationBudgetTracker {

	/** Ticks between periodic reports (~60 s at 60 fps). */
	private static final int REPORT_INTERVAL_TICKS = 3600;

	/**
	 * Default per-tick budget: 8 ms. At 60 fps the total frame budget is ~16.7 ms;
	 * allowing the simulation half keeps the render thread unblocked.
	 */
	private static final float DEFAULT_BUDGET_MS = 8.0f;

	private final float budgetMs;

	private long tickCount = 0;
	private long overrunCount = 0;
	private float maxObservedMs = 0f;
	private float sumMs = 0f;
	private int reportCounter = 0;

	/** Creates a tracker with the default 8 ms budget. */
	public SimulationBudgetTracker() {
		this(DEFAULT_BUDGET_MS);
	}

	/**
	 * Creates a tracker with a custom budget.
	 *
	 * @param budgetMs per-tick budget in milliseconds
	 */
	public SimulationBudgetTracker(final float budgetMs) {
		this.budgetMs = budgetMs;
	}

	/**
	 * Records the start time of a simulation tick.
	 *
	 * @return {@link System#nanoTime()} snapshot to pass to {@link #endTick(long)}
	 */
	public long beginTick() {
		return System.nanoTime();
	}

	/**
	 * Records the end of a simulation tick and updates statistics.
	 *
	 * @param startNanos value returned by the preceding {@link #beginTick()} call
	 */
	public void endTick(final long startNanos) {
		final float ms = (System.nanoTime() - startNanos) / 1_000_000f;
		tickCount++;
		sumMs += ms;
		if (ms > maxObservedMs) {
			maxObservedMs = ms;
		}
		if (ms > budgetMs) {
			overrunCount++;
		}
		reportCounter++;
		if (reportCounter >= REPORT_INTERVAL_TICKS) {
			report();
			reportCounter = 0;
		}
	}

	private void report() {
		if (tickCount == 0) {
			return;
		}
		final float avgMs = sumMs / tickCount;
		final float overrunPct = 100.0f * overrunCount / tickCount;
		System.out.printf(
				"[SimBudget] avg=%.2f ms  max=%.2f ms  budget=%.2f ms  overruns=%d/%d (%.1f%%)%n",
				avgMs, maxObservedMs, budgetMs, overrunCount, tickCount, overrunPct);
		if (overrunPct > 10f) {
			System.out.printf(
					"[SimBudget] WARNING: simulation exceeds budget in %.1f%% of ticks — consider profiling CSimulation.step()%n",
					overrunPct);
		}
		// Reset rolling stats so the next window reflects only recent behaviour.
		tickCount = 0;
		sumMs = 0f;
		maxObservedMs = 0f;
		overrunCount = 0;
	}
}
