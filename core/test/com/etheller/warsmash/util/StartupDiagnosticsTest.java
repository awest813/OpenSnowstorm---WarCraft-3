package com.etheller.warsmash.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class StartupDiagnosticsTest {

	private void assertVersion(final int expectedMajor, final int expectedMinor, final String input) {
		final int[] result = StartupDiagnostics.parseGLVersion(input);
		assertArrayEquals(
				new int[]{expectedMajor, expectedMinor},
				result,
				"parseGLVersion(\"" + input + "\")");
	}

	@Test
	void parsesNvidiaStyleVersion() {
		assertVersion(4, 6, "4.6.0 NVIDIA 531.18");
	}

	@Test
	void parsesIntelDCHVersion() {
		assertVersion(3, 3, "3.3.0 - Build 10.18.15.5074");
	}

	@Test
	void parsesMesaVersion() {
		assertVersion(3, 3, "3.3 Mesa 22.3.5");
	}

	@Test
	void parsesMinimumGL33() {
		assertVersion(3, 3, "3.3.0");
	}

	@Test
	void parsesGL21() {
		assertVersion(2, 1, "2.1.0");
	}

	@Test
	void parsesGLVersionWithSpaces() {
		assertVersion(4, 5, "4.5 ATI-4.5.14");
	}

	@Test
	void nullVersionReturnsZeroZero() {
		assertVersion(0, 0, null);
	}

	@Test
	void emptyStringReturnsZeroZero() {
		assertVersion(0, 0, "");
	}

	@Test
	void garbledStringReturnsZeroZero() {
		assertVersion(0, 0, "not-a-version");
	}
}
