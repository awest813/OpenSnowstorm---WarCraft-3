package com.etheller.warsmash.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ObjectPoolTest {

	@Test
	void acquireReturnsNonNullObjects() {
		final ObjectPool<StringBuilder> pool = new ObjectPool<>(4, StringBuilder::new);
		final StringBuilder obj = pool.acquire();
		assertNotNull(obj);
	}

	@Test
	void releasedObjectIsReusedOnNextAcquire() {
		final ObjectPool<StringBuilder> pool = new ObjectPool<>(4, StringBuilder::new);
		final StringBuilder first = pool.acquire();
		pool.release(first);
		final StringBuilder second = pool.acquire();
		assertSame(first, second, "pool should return the previously released instance");
	}

	@Test
	void overflowBeyondCapacityDoesNotThrow() {
		final ObjectPool<Object> pool = new ObjectPool<>(2, Object::new);
		final Object a = pool.acquire();
		final Object b = pool.acquire();
		final Object c = pool.acquire();
		pool.release(a);
		pool.release(b);
		pool.release(c);
		assertEquals(2, pool.available(), "overflow objects are silently discarded");
	}

	@Test
	void hitRateIsZeroBeforeAnyAcquires() {
		final ObjectPool<Object> pool = new ObjectPool<>(4, Object::new);
		assertEquals(0.0, pool.hitRate(), 0.0001);
	}

	@Test
	void hitRateApproachesOneAfterPrePopulatingPool() {
		final ObjectPool<StringBuilder> pool = new ObjectPool<>(8, StringBuilder::new);

		// Pre-populate pool by acquiring and releasing.
		final List<StringBuilder> buf = new ArrayList<>();
		for (int i = 0; i < 8; i++) {
			buf.add(pool.acquire());
		}
		for (final StringBuilder sb : buf) {
			pool.release(sb);
		}
		pool.resetStats();

		// All 8 subsequent acquires should be cache-hits.
		for (int i = 0; i < 8; i++) {
			pool.acquire();
		}
		assertEquals(1.0, pool.hitRate(), 0.0001, "all acquires after pre-population should be hits");
	}

	@Test
	void availableCountTracksReleases() {
		final ObjectPool<Object> pool = new ObjectPool<>(4, Object::new);
		assertEquals(0, pool.available());
		final Object obj = pool.acquire();
		assertEquals(0, pool.available());
		pool.release(obj);
		assertEquals(1, pool.available());
	}

	@Test
	void freshAllocationsHitRateIsZero() {
		final ObjectPool<Object> pool = new ObjectPool<>(4, Object::new);
		pool.acquire();
		pool.acquire();
		pool.acquire();
		assertEquals(0.0, pool.hitRate(), 0.0001, "no releases yet means all are new allocations");
	}

	@Test
	void resetStatsClearsHitRateWithoutDrainingPool() {
		final ObjectPool<StringBuilder> pool = new ObjectPool<>(4, StringBuilder::new);
		final StringBuilder sb = pool.acquire();
		pool.release(sb);
		final StringBuilder sb2 = pool.acquire();
		pool.release(sb2);
		assertEquals(1, pool.available(), "pool should contain one object before resetStats");
		pool.resetStats();
		assertEquals(0.0, pool.hitRate(), 0.0001, "hit rate should be 0 after resetStats");
		assertEquals(1, pool.available(), "pool contents should survive resetStats");
	}

	@Test
	void distinctFactoryObjectsReturnedWhenPoolEmpty() {
		final ObjectPool<StringBuilder> pool = new ObjectPool<>(2, StringBuilder::new);
		final StringBuilder a = pool.acquire();
		final StringBuilder b = pool.acquire();
		assertNotSame(a, b, "two cold acquires must produce two distinct objects");
	}
}
