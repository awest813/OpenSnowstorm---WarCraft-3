package com.etheller.warsmash.desktop;

import static org.lwjgl.openal.AL10.AL_ORIENTATION;
import static org.lwjgl.openal.AL10.AL_POSITION;
import static org.lwjgl.openal.AL10.alListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL33;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.LwjglNativesLoader;
import com.etheller.warsmash.WarsmashGdxFDFTestRenderScreen;
import com.etheller.warsmash.WarsmashGdxMenuScreen;
import com.etheller.warsmash.WarsmashGdxMultiScreenGame;
import com.etheller.warsmash.audio.OpenALSound;
import com.etheller.warsmash.units.DataTable;
import com.etheller.warsmash.units.Element;
import com.etheller.warsmash.util.StringBundle;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.AudioContext;
import com.etheller.warsmash.viewer5.AudioContext.Listener;
import com.etheller.warsmash.viewer5.AudioDestination;
import com.etheller.warsmash.viewer5.gl.ANGLEInstancedArrays;
import com.etheller.warsmash.viewer5.gl.AudioExtension;
import com.etheller.warsmash.viewer5.gl.DynamicShadowExtension;
import com.etheller.warsmash.viewer5.gl.Extensions;
import com.etheller.warsmash.viewer5.gl.WireframeExtension;

public class DesktopLauncher {
	private static final int DEFAULT_WINDOWED_WIDTH = 1280;
	private static final int DEFAULT_WINDOWED_HEIGHT = 720;
	private static final String DEFAULT_LOG_DIRECTORY = "Logs";

	private enum LaunchProfile {
		SAFE, BALANCED, HIGH;

		static LaunchProfile parse(final String name) {
			switch (name.toLowerCase(java.util.Locale.ROOT)) {
			case "safe":
				return SAFE;
			case "balanced":
				return BALANCED;
			case "high":
				return HIGH;
			default:
				System.err.println("Unknown profile '" + name + "'. Valid values: safe, balanced, high. Using balanced.");
				return BALANCED;
			}
		}
	}

	public static void main(final String[] arg) {
		final String[] normalizedArgs = normalizeArguments(arg);
		System.out.println("Warsmash engine is starting...");
		if (Arrays.asList(normalizedArgs).contains("-validate") || Arrays.asList(normalizedArgs).contains("--validate")) {
			final String iniPath = findIniPathArg(normalizedArgs);
			final DataTable warsmashIni = loadWarsmashIni(iniPath);
			validateAndExit(warsmashIni, iniPath);
		}
		final LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.useGL30 = true;
		config.gles30ContextMajorVersion = 3;
		config.gles30ContextMinorVersion = 3;
//		config.samples = 16;
//		config.vSyncEnabled = false;
		config.addIcon("resources/Icon16.png", Files.FileType.Internal);
		config.addIcon("resources/Icon32.png", Files.FileType.Internal);
		config.addIcon("resources/Icon64.png", Files.FileType.Internal);
		config.addIcon("resources/Icon128.png", Files.FileType.Internal);
//		config.foregroundFPS = 0;
//		config.backgroundFPS = 0;
		config.width = DEFAULT_WINDOWED_WIDTH;
		config.height = DEFAULT_WINDOWED_HEIGHT;
		config.fullscreen = true;
		String fileToLoad = null;
		String iniPath = null;
		String logDirectory = DEFAULT_LOG_DIRECTORY;
		boolean noLogs = false;
		Boolean windowedMode = null;
		Integer windowedWidth = null;
		Integer windowedHeight = null;
		Integer targetFps = null;
		Integer msaaSamples = null;
		Boolean vSyncEnabled = null;
		LaunchProfile profile = null;
		final List<String> unknownArgs = new ArrayList<>();
		for (int argIndex = 0; argIndex < normalizedArgs.length; argIndex++) {
			if ("-help".equals(normalizedArgs[argIndex]) || "--help".equals(normalizedArgs[argIndex]) || "-h".equals(normalizedArgs[argIndex])) {
				printHelpAndExit();
			}
			else if ((normalizedArgs.length > (argIndex + 1)) && ("-profile".equals(normalizedArgs[argIndex]) || "--profile".equals(normalizedArgs[argIndex]))) {
				argIndex++;
				profile = LaunchProfile.parse(normalizedArgs[argIndex]);
			}
			else if ("-window".equals(normalizedArgs[argIndex]) || "-windowed".equals(normalizedArgs[argIndex])
					|| "--windowed".equals(normalizedArgs[argIndex])) {
				windowedMode = Boolean.TRUE;
				if ((normalizedArgs.length > (argIndex + 2)) && isInteger(normalizedArgs[argIndex + 1]) && isInteger(normalizedArgs[argIndex + 2])) {
					argIndex++;
					windowedWidth = parseIntWithFallback(normalizedArgs[argIndex], DEFAULT_WINDOWED_WIDTH, "window width", 1);
					argIndex++;
					windowedHeight = parseIntWithFallback(normalizedArgs[argIndex], DEFAULT_WINDOWED_HEIGHT, "window height", 1);
				}
				else {
					windowedWidth = DEFAULT_WINDOWED_WIDTH;
					windowedHeight = DEFAULT_WINDOWED_HEIGHT;
				}
			}
			else if ((normalizedArgs.length > (argIndex + 1)) && ("-width".equals(normalizedArgs[argIndex]) || "--width".equals(normalizedArgs[argIndex]))) {
				argIndex++;
				windowedWidth = parseIntWithFallback(normalizedArgs[argIndex], DEFAULT_WINDOWED_WIDTH, "window width", 1);
				if (windowedMode == null) {
					windowedMode = Boolean.TRUE;
				}
			}
			else if ((normalizedArgs.length > (argIndex + 1))
					&& ("-height".equals(normalizedArgs[argIndex]) || "--height".equals(normalizedArgs[argIndex]))) {
				argIndex++;
				windowedHeight = parseIntWithFallback(normalizedArgs[argIndex], DEFAULT_WINDOWED_HEIGHT, "window height", 1);
				if (windowedMode == null) {
					windowedMode = Boolean.TRUE;
				}
			}
			else if ("-fullscreen".equals(normalizedArgs[argIndex]) || "--fullscreen".equals(normalizedArgs[argIndex])) {
				windowedMode = Boolean.FALSE;
			}
			else if ("-nolog".equals(normalizedArgs[argIndex])) {
				noLogs = true;
			}
			else if ("-log".equals(normalizedArgs[argIndex]) || "--log".equals(normalizedArgs[argIndex])) {
				noLogs = false;
			}
			else if ((normalizedArgs.length > (argIndex + 1))
					&& ("-logdir".equals(normalizedArgs[argIndex]) || "--logdir".equals(normalizedArgs[argIndex]))) {
				argIndex++;
				logDirectory = normalizedArgs[argIndex];
			}
			else if ("-vsync".equals(normalizedArgs[argIndex])) {
				vSyncEnabled = Boolean.TRUE;
			}
			else if ("-novsync".equals(normalizedArgs[argIndex])) {
				vSyncEnabled = Boolean.FALSE;
			}
			else if ((normalizedArgs.length > (argIndex + 1)) && ("-fps".equals(normalizedArgs[argIndex]) || "--fps".equals(normalizedArgs[argIndex]))) {
				argIndex++;
				targetFps = parseIntWithFallback(normalizedArgs[argIndex], 0, "target FPS", 0);
			}
			else if ((normalizedArgs.length > (argIndex + 1))
					&& ("-msaa".equals(normalizedArgs[argIndex]) || "--msaa".equals(normalizedArgs[argIndex]))) {
				argIndex++;
				msaaSamples = parseIntWithFallback(normalizedArgs[argIndex], 0, "MSAA samples", 0);
			}
			else if ((normalizedArgs.length > (argIndex + 1))
					&& ("-loadfile".equals(normalizedArgs[argIndex]) || "--loadfile".equals(normalizedArgs[argIndex]))) {
				argIndex++;
				fileToLoad = normalizedArgs[argIndex];
			}
			else if ((normalizedArgs.length > (argIndex + 1)) && ("-ini".equals(normalizedArgs[argIndex]) || "--ini".equals(normalizedArgs[argIndex]))) {
				argIndex++;
				iniPath = normalizedArgs[argIndex];
			}
			else if (isOptionRequiringValue(normalizedArgs[argIndex])) {
				System.err.println("Missing value for launcher option: " + normalizedArgs[argIndex]);
			}
			else {
				unknownArgs.add(normalizedArgs[argIndex]);
			}
		}
		if (!unknownArgs.isEmpty()) {
			System.err.println("Warning: unknown launcher option(s): " + String.join(", ", unknownArgs));
			System.err.println("Use -help to see available options.");
		}
		if (profile != null) {
			applyProfile(profile, config);
		}
		// Individual flags override profile settings.
		if (Boolean.TRUE.equals(windowedMode)) {
			config.fullscreen = false;
			config.width = (windowedWidth != null) ? windowedWidth : DEFAULT_WINDOWED_WIDTH;
			config.height = (windowedHeight != null) ? windowedHeight : DEFAULT_WINDOWED_HEIGHT;
		}
		else if (Boolean.FALSE.equals(windowedMode)) {
			config.fullscreen = true;
		}
		if (vSyncEnabled != null) {
			config.vSyncEnabled = vSyncEnabled;
		}
		if (targetFps != null) {
			config.foregroundFPS = Math.max(0, targetFps);
			config.backgroundFPS = Math.max(0, targetFps);
		}
		if (msaaSamples != null) {
			config.samples = Math.max(0, msaaSamples);
		}
		if (config.fullscreen) {
			final DisplayMode desktopDisplayMode = LwjglApplicationConfiguration.getDesktopDisplayMode();
			config.width = desktopDisplayMode.width;
			config.height = desktopDisplayMode.height;
		}
		printLaunchSummary(config, profile, iniPath, fileToLoad, noLogs, logDirectory);
		if (!noLogs) {
			new File(logDirectory).mkdirs();
			final long logTimestamp = System.currentTimeMillis();
			try {
				System.setOut(new PrintStream(
						new FileOutputStream(new File(logDirectory + "/" + logTimestamp + ".out.log"))));
			}
			catch (final FileNotFoundException e) {
				e.printStackTrace();
			}
			try {
				System.setErr(new PrintStream(
						new FileOutputStream(new File(logDirectory + "/" + logTimestamp + ".err.log"))));
			}
			catch (final FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		loadExtensions();
		final DataTable warsmashIni = loadWarsmashIni(iniPath);
		final Element emulatorConstants = warsmashIni.get("Emulator");
		WarsmashConstants.loadConstants(emulatorConstants, warsmashIni);

		if (fileToLoad != null) {
			System.out.println("About to run loading file: " + fileToLoad);
		}
		final WarsmashGdxMultiScreenGame warsmashGdxMultiScreenGame = new WarsmashGdxMultiScreenGame();
		new LwjglApplication(warsmashGdxMultiScreenGame, config);
		final String finalFileToLoad = fileToLoad;
		Gdx.app.postRunnable(new Runnable() {
			@Override
			public void run() {
				if ((finalFileToLoad != null) && finalFileToLoad.toLowerCase().endsWith(".toc")) {
					warsmashGdxMultiScreenGame.setScreen(new WarsmashGdxFDFTestRenderScreen(warsmashIni,
							warsmashGdxMultiScreenGame, finalFileToLoad));
				}
				else {
					final WarsmashGdxMenuScreen menuScreen = new WarsmashGdxMenuScreen(warsmashIni,
							warsmashGdxMultiScreenGame);
					warsmashGdxMultiScreenGame.setScreen(menuScreen);
					if (finalFileToLoad != null) {
						menuScreen.startMap(finalFileToLoad);
					}
				}
			}
		});
	}

	private static void applyProfile(final LaunchProfile profile, final LwjglApplicationConfiguration config) {
		switch (profile) {
		case SAFE:
			System.out.println("[Profile] safe — reduced effects, no MSAA, windowed 1280x720, vsync on");
			config.fullscreen = false;
			config.width = DEFAULT_WINDOWED_WIDTH;
			config.height = DEFAULT_WINDOWED_HEIGHT;
			config.samples = 0;
			config.vSyncEnabled = true;
			config.foregroundFPS = 60;
			config.backgroundFPS = 60;
			break;
		case BALANCED:
			System.out.println("[Profile] balanced — windowed 1280x720, 2x MSAA, vsync on, 60 fps cap");
			config.fullscreen = false;
			config.width = DEFAULT_WINDOWED_WIDTH;
			config.height = DEFAULT_WINDOWED_HEIGHT;
			config.samples = 2;
			config.vSyncEnabled = true;
			config.foregroundFPS = 60;
			config.backgroundFPS = 30;
			break;
		case HIGH:
			System.out.println("[Profile] high — fullscreen, 4x MSAA, vsync on, uncapped fps");
			config.samples = 4;
			config.vSyncEnabled = true;
			config.foregroundFPS = 0;
			config.backgroundFPS = 0;
			break;
		default:
			break;
		}
	}

	private static void printHelpAndExit() {
		System.out.println("Warsmash desktop launcher options:");
		System.out.println("  -help | --help | -h          Show this help message");
		System.out.println("  -profile <name>              Apply a preset: safe | balanced | high");
		System.out.println("  -window | -windowed [w h]    Run in windowed mode (defaults to 1280x720)");
		System.out.println("  -width <pixels>              Set window width (implies windowed mode)");
		System.out.println("  -height <pixels>             Set window height (implies windowed mode)");
		System.out.println("  -fullscreen                  Force fullscreen mode");
		System.out.println("  -vsync | -novsync            Force VSync on or off");
		System.out.println("  -fps <value>                 Limit foreground/background FPS (0 = uncapped)");
		System.out.println("  -msaa <samples>              Set MSAA sample count (example: 4)");
		System.out.println("  -ini <path>                  Use a custom warsmash ini file");
		System.out.println("  -loadfile <path>             Auto-load a map or toc file");
		System.out.println("  -nolog                       Keep stdout/stderr in console");
		System.out.println("  -logdir <path>               Store .out/.err logs in a custom directory");
		System.out.println("  Long options also accept --name=value syntax");
		System.out.println("  -validate | --validate       Check warsmash.ini asset paths and exit");
		System.exit(0);
	}

	private static boolean isInteger(final String value) {
		try {
			Integer.parseInt(value);
			return true;
		}
		catch (final NumberFormatException exc) {
			return false;
		}
	}

	private static int parseIntWithFallback(final String value, final int fallback, final String optionName,
			final int minimumValue) {
		try {
			final int parsedValue = Integer.parseInt(value);
			if (parsedValue < minimumValue) {
				System.err.println("Invalid value for " + optionName + ": '" + value + "'. Minimum is "
						+ minimumValue + ". Using " + fallback + '.');
				return fallback;
			}
			return parsedValue;
		}
		catch (final NumberFormatException exc) {
			System.err.println("Invalid value for " + optionName + ": '" + value + "'. Using " + fallback + '.');
			return fallback;
		}
	}

	private static void printLaunchSummary(final LwjglApplicationConfiguration config, final LaunchProfile profile,
			final String iniPath, final String fileToLoad, final boolean noLogs, final String logDirectory) {
		System.out.println("Launch configuration:");
		System.out.println("  Profile: " + ((profile == null) ? "custom/default" : profile.name().toLowerCase(java.util.Locale.ROOT)));
		System.out.println("  Display: " + (config.fullscreen ? "fullscreen" : ("windowed " + config.width + "x" + config.height)));
		System.out.println("  VSync: " + (config.vSyncEnabled ? "enabled" : "disabled"));
		System.out.println("  FPS cap: fg=" + config.foregroundFPS + ", bg=" + config.backgroundFPS);
		System.out.println("  MSAA samples: " + config.samples);
		System.out.println("  INI: " + ((iniPath == null) ? "warsmash.ini" : iniPath));
		System.out.println("  Auto-load: " + ((fileToLoad == null) ? "none" : fileToLoad));
		System.out.println("  Logging: " + (noLogs ? "stdout/stderr console" : ("files in " + logDirectory)));
	}

	private static String[] normalizeArguments(final String[] arg) {
		final List<String> normalized = new ArrayList<>();
		for (final String value : arg) {
			if (value.startsWith("--") && value.contains("=")) {
				final int equalsIndex = value.indexOf('=');
				final String optionName = value.substring(0, equalsIndex);
				final String optionValue = value.substring(equalsIndex + 1);
				normalized.add(optionName);
				normalized.add(optionValue);
			}
			else {
				normalized.add(value);
			}
		}
		return normalized.toArray(new String[0]);
	}

	private static boolean isOptionRequiringValue(final String option) {
		return "-profile".equals(option) || "--profile".equals(option) || "-width".equals(option)
				|| "--width".equals(option) || "-height".equals(option) || "--height".equals(option)
				|| "-fps".equals(option) || "--fps".equals(option) || "-msaa".equals(option)
				|| "--msaa".equals(option) || "-loadfile".equals(option) || "--loadfile".equals(option)
				|| "-ini".equals(option) || "--ini".equals(option) || "-logdir".equals(option)
				|| "--logdir".equals(option);
	}

	private static String findIniPathArg(final String[] arg) {
		for (int i = 0; i < arg.length - 1; i++) {
			if ("-ini".equals(arg[i]) || "--ini".equals(arg[i])) {
				return arg[i + 1];
			}
		}
		return null;
	}

	private static void validateAndExit(final DataTable warsmashIni, final String iniPath) {
		final String resolvedIni = iniPath != null ? iniPath : "warsmash.ini";
		System.out.println("Validating data sources from: " + resolvedIni);
		final com.etheller.warsmash.units.Element dataSourcesConfig = warsmashIni.get("DataSources");
		if (dataSourcesConfig == null) {
			System.out.println("ERROR: [DataSources] section not found in INI.");
			System.exit(1);
		}
		int total = 0;
		int failures = 0;
		for (int i = 0; i < dataSourcesConfig.size(); i++) {
			final String indexStr = (i < 10 ? "0" : "") + i;
			final String type = dataSourcesConfig.getField("Type" + indexStr);
			final String path = dataSourcesConfig.getField("Path" + indexStr);
			if ((type == null) || type.isEmpty()) {
				continue;
			}
			total++;
			final File f = new File(path);
			final boolean exists = f.exists();
			final String status = exists ? "OK  " : "FAIL";
			System.out.println("  [" + status + "] " + type + ": " + path);
			if (!exists) {
				failures++;
			}
		}
		System.out.println("Result: " + (total - failures) + "/" + total + " data source(s) found.");
		System.exit(failures > 0 ? 1 : 0);
	}

	public static DataTable loadWarsmashIni(final String iniPath) {
		final DataTable warsmashIni = new DataTable(StringBundle.EMPTY);
		try (FileInputStream warsmashIniInputStream = new FileInputStream(iniPath != null ? iniPath : "warsmash.ini")) {
			warsmashIni.readTXT(warsmashIniInputStream, true);
		}
		catch (final FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		catch (final IOException e) {
			throw new RuntimeException(e);
		}
		return warsmashIni;
	}

	public static DataTable loadWarsmashIni() {
		return loadWarsmashIni(null);
	}

	public static void loadExtensions() {
		LwjglNativesLoader.load();
		Extensions.angleInstancedArrays = new ANGLEInstancedArrays() {
			@Override
			public void glVertexAttribDivisorANGLE(final int index, final int divisor) {
				GL33.glVertexAttribDivisor(index, divisor);
			}

			@Override
			public void glDrawElementsInstancedANGLE(final int mode, final int count, final int type,
					final int indicesOffset, final int instanceCount) {
				GL31.glDrawElementsInstanced(mode, count, type, indicesOffset, instanceCount);
			}

			@Override
			public void glDrawArraysInstancedANGLE(final int mode, final int first, final int count,
					final int instanceCount) {
				GL31.glDrawArraysInstanced(mode, first, count, instanceCount);
			}
		};
		Extensions.dynamicShadowExtension = new DynamicShadowExtension() {
			@Override
			public void glFramebufferTexture(final int target, final int attachment, final int texture,
					final int level) {
				GL32.glFramebufferTexture(target, attachment, texture, level);
			}

			@Override
			public void glDrawBuffer(final int mode) {
				GL11.glDrawBuffer(mode);
			}
		};
		Extensions.wireframeExtension = new WireframeExtension() {
			@Override
			public void glPolygonMode(final int face, final int mode) {
				GL11.glPolygonMode(face, mode);
			}
		};
		Extensions.audio = new AudioExtension() {
			final FloatBuffer orientation = BufferUtils.createFloatBuffer(6).clear();
			final FloatBuffer position = BufferUtils.createFloatBuffer(3).clear();

			@Override
			public float getDuration(final Sound sound) {
				if (sound == null) {
					return 1;
				}
				return ((OpenALSound) sound).duration();
			}

			@Override
			public long play(final Sound buffer, final float volume, final float pitch, final float x, final float y,
					final float z, final boolean is3dSound, final float maxDistance, final float refDistance,
					final boolean looping) {
				return ((OpenALSound) buffer).play(volume, pitch, x, y, z, is3dSound, maxDistance, refDistance, looping);
			}

			@Override
			public AudioContext createContext(final boolean world) {
				Listener listener;
				if (world && AL.isCreated()) {
					listener = new Listener() {
						private float x;
						private float y;
						private float z;

						@Override
						public void setPosition(final float x, final float y, final float z) {
							this.x = x;
							this.y = y;
							this.z = z;
							position.put(0, x);
							position.put(1, y);
							position.put(2, z);
							alListener(AL_POSITION, position);
						}

						@Override
						public float getX() {
							return this.x;
						}

						@Override
						public float getY() {
							return this.y;
						}

						@Override
						public float getZ() {
							return this.z;
						}

						@Override
						public void setOrientation(final float forwardX, final float forwardY, final float forwardZ,
								final float upX, final float upY, final float upZ) {
							orientation.put(0, forwardX);
							orientation.put(1, forwardY);
							orientation.put(2, forwardZ);
							orientation.put(3, upX);
							orientation.put(4, upY);
							orientation.put(5, upZ);
							alListener(AL_ORIENTATION, orientation);
						}

						@Override
						public boolean is3DSupported() {
							return true;
						}
					};
				}
				else {
					listener = Listener.DO_NOTHING;
				}

				return new AudioContext(listener, new AudioDestination() {
				});
			}
		};
		Extensions.GL_LINE = GL11.GL_LINE;
		Extensions.GL_FILL = GL11.GL_FILL;
	}
}
