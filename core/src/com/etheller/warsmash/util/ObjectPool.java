package com.etheller.warsmash.util;

import java.util.function.Supplier;

/**
 * A simple fixed-capacity stack-based object pool for reducing GC pressure in
 * hot paths.
 *
 * <p>Usage pattern:
 * <pre>
 *   ObjectPool&lt;Foo&gt; pool = new ObjectPool&lt;&gt;(64, Foo::new);
 *
 *   // acquire (reuses a released object or allocates a fresh one)
 *   Foo obj = pool.acquire();
 *   obj.init(...);
 *   // ... use obj ...
 *   pool.release(obj);   // return to pool for reuse
 * </pre>
 *
 * <p>Objects released beyond the pool's capacity are simply discarded and
 * become eligible for normal GC — no overflow exception is thrown.
 *
 * <p>This class is <em>not</em> thread-safe. All acquire/release calls must
 * occur on the same thread (the LibGDX render thread).
 *
 * @param <T> the pooled object type
 */
public final class ObjectPool<T> {

	private final Object[] pool;
	private int top = 0;

	private final Supplier<T> factory;

	/** Total calls to {@link #acquire()} since construction. */
	private long totalAcquired = 0;
	/** Total times {@link #acquire()} had to allocate a new object. */
	private long totalNewAllocations = 0;

	/**
	 * @param capacity maximum number of objects retained between uses
	 * @param factory  creates a fresh object when the pool is empty
	 */
	public ObjectPool(final int capacity, final Supplier<T> factory) {
		this.pool = new Object[capacity];
		this.factory = factory;
	}

	/**
	 * Returns a recycled object from the pool, or a freshly allocated one if the
	 * pool is empty.
	 */
	@SuppressWarnings("unchecked")
	public T acquire() {
		totalAcquired++;
		if (top > 0) {
			final T obj = (T) pool[--top];
			pool[top] = null;
			return obj;
		}
		totalNewAllocations++;
		return factory.get();
	}

	/**
	 * Returns {@code obj} to the pool. If the pool is already at capacity the
	 * object is silently dropped.
	 */
	public void release(final T obj) {
		if (top < pool.length) {
			pool[top++] = obj;
		}
	}

	/** Number of objects currently sitting in the pool. */
	public int available() {
		return top;
	}

	/**
	 * Fraction of {@link #acquire()} calls satisfied from the pool rather than
	 * allocating (0.0 – 1.0). Returns {@code 0} before any calls.
	 */
	public double hitRate() {
		if (totalAcquired == 0) {
			return 0.0;
		}
		return 1.0 - ((double) totalNewAllocations / totalAcquired);
	}

	/** Resets the hit-rate counters without emptying the pool. */
	public void resetStats() {
		totalAcquired = 0;
		totalNewAllocations = 0;
	}
}
