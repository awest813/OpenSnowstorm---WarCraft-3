package com.etheller.warsmash.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;

/**
 * Logs hardware and driver capabilities to stdout at startup.
 * Call {@link #reportGLCapabilities()} once a GL context is available (inside
 * the LibGDX {@code create()} callback).
 *
 * <p>Also provides {@link #checkGLRequirements()} which verifies that the
 * OpenGL version meets the minimum required by Warsmash (3.3 Core).
 */
public final class StartupDiagnostics {

	private static final int GL_SHADING_LANGUAGE_VERSION = 0x8B8C;

	/** Minimum required OpenGL major version. */
	private static final int REQUIRED_GL_MAJOR = 3;
	/** Minimum required OpenGL minor version (when major == REQUIRED_GL_MAJOR). */
	private static final int REQUIRED_GL_MINOR = 3;

	private StartupDiagnostics() {
	}

	public static void reportGLCapabilities() {
		final String vendor = Gdx.gl.glGetString(GL20.GL_VENDOR);
		final String renderer = Gdx.gl.glGetString(GL20.GL_RENDERER);
		final String glVersion = Gdx.gl.glGetString(GL20.GL_VERSION);
		final String glslVersion = Gdx.gl.glGetString(GL_SHADING_LANGUAGE_VERSION);
		final int displayW = Gdx.graphics.getWidth();
		final int displayH = Gdx.graphics.getHeight();
		final int refreshRate = Gdx.graphics.getDisplayMode().refreshRate;

		System.out.println("=== Warsmash Startup Capability Report ===");
		System.out.println("Java:        " + System.getProperty("java.version")
				+ " (" + System.getProperty("java.vm.name") + ")");
		System.out.println("OS:          " + System.getProperty("os.name")
				+ " " + System.getProperty("os.version")
				+ " [" + System.getProperty("os.arch") + "]");
		System.out.println("GL Vendor:   " + vendor);
		System.out.println("GL Renderer: " + renderer);
		System.out.println("GL Version:  " + glVersion);
		System.out.println("GLSL:        " + glslVersion);
		System.out.println("Display:     " + displayW + "x" + displayH + " @ " + refreshRate + " Hz");
		System.out.println("==========================================");
	}

	/**
	 * Checks that the OpenGL context version satisfies the Warsmash minimum
	 * requirement of OpenGL 3.3. If the requirement is not met, a clear
	 * user-facing error is printed to stderr and the JVM exits with code 1.
	 *
	 * <p>Call this immediately after {@link #reportGLCapabilities()} so the
	 * diagnostic report is always visible before any exit.
	 */
	public static void checkGLRequirements() {
		final String glVersion = Gdx.gl.glGetString(GL20.GL_VERSION);
		final int[] parsed = parseGLVersion(glVersion);
		final int major = parsed[0];
		final int minor = parsed[1];

		final boolean sufficient = (major > REQUIRED_GL_MAJOR)
				|| ((major == REQUIRED_GL_MAJOR) && (minor >= REQUIRED_GL_MINOR));

		if (!sufficient) {
			System.err.println();
			System.err.println("*** Warsmash: OpenGL version too old ***");
			System.err.println("  Detected:  OpenGL " + major + "." + minor
					+ " (\"" + glVersion + "\")");
			System.err.println("  Required:  OpenGL " + REQUIRED_GL_MAJOR + "." + REQUIRED_GL_MINOR + " Core or higher");
			System.err.println();
			System.err.println("Possible fixes:");
			System.err.println("  - Update your graphics driver to the latest version.");
			System.err.println("  - On Intel integrated graphics, use the official Intel DCH driver.");
			System.err.println("  - On a laptop, ensure Warsmash runs on the discrete GPU, not integrated graphics.");
			System.err.println("  - If using a VM or WSL, hardware-accelerated GL may not be supported.");
			System.err.println();
			System.exit(1);
		}

		System.out.println("[GL] OpenGL " + major + "." + minor + " — requirement (3.3) satisfied.");
	}

	/**
	 * Parses the leading "MAJOR.MINOR" from a GL version string such as
	 * {@code "4.6.0 NVIDIA 531.18"} or {@code "3.3 Mesa 22.3.5"}.
	 *
	 * <p>Only leading digits are consumed for each component so that suffixes
	 * like {@code " NVIDIA"}, {@code " ATI-4.5.14"}, or {@code ".0 - Build"}
	 * do not corrupt the parse.
	 *
	 * @return int[]{major, minor}; falls back to {0, 0} on parse failure
	 */
	static int[] parseGLVersion(final String glVersionString) {
		if (glVersionString == null) {
			return new int[]{0, 0};
		}
		final String trimmed = glVersionString.trim();
		final int firstDot = trimmed.indexOf('.');
		if (firstDot < 0) {
			return new int[]{0, 0};
		}
		try {
			final String majorStr = extractLeadingDigits(trimmed.substring(0, firstDot));
			if (majorStr.isEmpty()) {
				return new int[]{0, 0};
			}
			final int major = Integer.parseInt(majorStr);
			final String minorStr = extractLeadingDigits(trimmed.substring(firstDot + 1));
			if (minorStr.isEmpty()) {
				return new int[]{0, 0};
			}
			final int minor = Integer.parseInt(minorStr);
			return new int[]{major, minor};
		}
		catch (final NumberFormatException e) {
			return new int[]{0, 0};
		}
	}

	private static String extractLeadingDigits(final String s) {
		int end = 0;
		while ((end < s.length()) && Character.isDigit(s.charAt(end))) {
			end++;
		}
		return (end > 0) ? s.substring(0, end) : "0";
	}
}
