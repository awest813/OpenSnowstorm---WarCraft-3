package com.etheller.warsmash.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SimulationBudgetTrackerTest {

	@Test
	void beginTickReturnsSensibleNanoTimeValue() {
		final SimulationBudgetTracker tracker = new SimulationBudgetTracker();
		final long t = tracker.beginTick();
		assertTrue(t > 0, "beginTick() should return a positive nano-time value");
	}

	@Test
	void endTickDoesNotThrowForZeroDuration() {
		final SimulationBudgetTracker tracker = new SimulationBudgetTracker();
		final long start = tracker.beginTick();
		tracker.endTick(start);
	}

	@Test
	void manyTicksDoNotThrow() {
		final SimulationBudgetTracker tracker = new SimulationBudgetTracker(1.0f);
		for (int i = 0; i < 4000; i++) {
			final long t = tracker.beginTick();
			tracker.endTick(t);
		}
	}

	@Test
	void customBudgetConstructorDoesNotThrow() {
		final SimulationBudgetTracker tracker = new SimulationBudgetTracker(16.0f);
		final long t = tracker.beginTick();
		tracker.endTick(t);
	}

	@Test
	void reportIntervalFiredAtCorrectTickCount() {
		// Drive exactly REPORT_INTERVAL_TICKS ticks (3600) so the report triggers.
		// We only verify that no exception is thrown and that the next tick works too.
		final SimulationBudgetTracker tracker = new SimulationBudgetTracker(8.0f);
		for (int i = 0; i < 3601; i++) {
			final long t = tracker.beginTick();
			tracker.endTick(t);
		}
	}
}
