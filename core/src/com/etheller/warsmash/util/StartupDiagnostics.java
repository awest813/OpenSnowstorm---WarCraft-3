package com.etheller.warsmash.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;

/**
 * Logs hardware and driver capabilities to stdout at startup.
 * Call {@link #reportGLCapabilities()} once a GL context is available (inside
 * the LibGDX {@code create()} callback).
 */
public final class StartupDiagnostics {

	private static final int GL_SHADING_LANGUAGE_VERSION = 0x8B8C;

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
}
