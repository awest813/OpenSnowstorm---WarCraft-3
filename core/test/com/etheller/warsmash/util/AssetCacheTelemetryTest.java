package com.etheller.warsmash.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AssetCacheTelemetryTest {

	@BeforeEach
	void resetState() {
		AssetCacheTelemetry.reset();
	}

	@Test
	void initialStateIsZero() {
		assertEquals(0, AssetCacheTelemetry.getTotalHits());
		assertEquals(0, AssetCacheTelemetry.getTotalMisses());
		assertEquals(0.0, AssetCacheTelemetry.hitRate(), 0.0001);
	}

	@Test
	void recordHitIncreasesHitCount() {
		AssetCacheTelemetry.recordHit("models/hero.mdx");
		assertEquals(1, AssetCacheTelemetry.getTotalHits());
		assertEquals(0, AssetCacheTelemetry.getTotalMisses());
	}

	@Test
	void recordMissIncreasesMissCount() {
		AssetCacheTelemetry.recordMiss("textures/wall.blp");
		assertEquals(0, AssetCacheTelemetry.getTotalHits());
		assertEquals(1, AssetCacheTelemetry.getTotalMisses());
	}

	@Test
	void hitRateCalculatedCorrectly() {
		AssetCacheTelemetry.recordHit("a");
		AssetCacheTelemetry.recordHit("b");
		AssetCacheTelemetry.recordHit("c");
		AssetCacheTelemetry.recordMiss("d");
		assertEquals(0.75, AssetCacheTelemetry.hitRate(), 0.0001);
	}

	@Test
	void resetClearsAllCounters() {
		AssetCacheTelemetry.recordHit("x");
		AssetCacheTelemetry.recordMiss("y");
		AssetCacheTelemetry.reset();
		assertEquals(0, AssetCacheTelemetry.getTotalHits());
		assertEquals(0, AssetCacheTelemetry.getTotalMisses());
		assertEquals(0.0, AssetCacheTelemetry.hitRate(), 0.0001);
	}

	@Test
	void reportDoesNotThrow() {
		AssetCacheTelemetry.recordHit("model.mdx");
		AssetCacheTelemetry.recordMiss("sound.mp3");
		AssetCacheTelemetry.report();
	}

	@Test
	void manyRecordsFireReportPeriodically() {
		// Verify that 50+ misses does not throw (periodic report fires at 50 misses).
		for (int i = 0; i < 60; i++) {
			AssetCacheTelemetry.recordMiss("asset_" + i);
		}
		assertEquals(60, AssetCacheTelemetry.getTotalMisses());
	}
}
