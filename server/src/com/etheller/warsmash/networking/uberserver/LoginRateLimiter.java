package com.etheller.warsmash.networking.uberserver;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-IP failed-login rate limiter using a leaky-bucket strategy.
 *
 * <p>After {@value #MAX_FAILURES} consecutive failures within a
 * {@value #WINDOW_MS}-ms window, further login and account-creation
 * requests from that address are rejected without touching user storage.
 * The bucket resets automatically after {@value #COOLDOWN_MS} ms of
 * silence from the offending address.
 *
 * <p>This class is not thread-safe; all calls must come from the single
 * server selector thread.
 */
public final class LoginRateLimiter {

	private static final int MAX_FAILURES = 5;
	private static final long WINDOW_MS = 60_000L;
	private static final long COOLDOWN_MS = 300_000L;

	private static final class Bucket {
		int failures;
		long windowStartMs;
		long blockedUntilMs;

		Bucket(final long now) {
			this.failures = 1;
			this.windowStartMs = now;
			this.blockedUntilMs = 0;
		}
	}

	private final Map<String, Bucket> buckets = new HashMap<>();

	/**
	 * Records a failed auth attempt for the given address.
	 * Must be called only when login or account-creation actually fails
	 * due to bad credentials (not for non-credential errors).
	 *
	 * @param address the remote address string
	 */
	public void recordFailure(final String address) {
		final long now = System.currentTimeMillis();
		Bucket bucket = this.buckets.get(address);
		if (bucket == null) {
			this.buckets.put(address, new Bucket(now));
			return;
		}
		if ((now - bucket.windowStartMs) > WINDOW_MS) {
			bucket.failures = 1;
			bucket.windowStartMs = now;
			bucket.blockedUntilMs = 0;
		}
		else {
			bucket.failures++;
			if (bucket.failures >= MAX_FAILURES) {
				bucket.blockedUntilMs = now + COOLDOWN_MS;
				System.out.printf(
						"[RateLimit] %s exceeded %d failures in %d s — blocked for %d s%n",
						address, MAX_FAILURES, WINDOW_MS / 1000, COOLDOWN_MS / 1000);
			}
		}
	}

	/**
	 * Records a successful login, clearing any accumulated failure count.
	 *
	 * @param address the remote address string
	 */
	public void recordSuccess(final String address) {
		this.buckets.remove(address);
	}

	/**
	 * Returns {@code true} if the address is currently rate-limited and
	 * the request should be rejected immediately without processing.
	 *
	 * @param address the remote address string
	 */
	public boolean isBlocked(final String address) {
		final Bucket bucket = this.buckets.get(address);
		if (bucket == null) {
			return false;
		}
		final long now = System.currentTimeMillis();
		if (bucket.blockedUntilMs > 0) {
			if (now < bucket.blockedUntilMs) {
				return true;
			}
			// Cooldown expired — clean up.
			this.buckets.remove(address);
		}
		return false;
	}
}
