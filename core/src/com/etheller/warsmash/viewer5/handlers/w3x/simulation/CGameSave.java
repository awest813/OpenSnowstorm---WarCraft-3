package com.etheller.warsmash.viewer5.handlers.w3x.simulation;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.GlobalScopeAssignable;
import com.etheller.interpreter.ast.value.BooleanJassValue;
import com.etheller.interpreter.ast.value.IntegerJassValue;
import com.etheller.interpreter.ast.value.JassValue;
import com.etheller.interpreter.ast.value.RealJassValue;
import com.etheller.interpreter.ast.value.StringJassValue;
import com.etheller.interpreter.ast.value.visitor.BooleanJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.StringJassValueVisitor;

/**
 * Manages on-disk save-game state for a running campaign mission.
 *
 * <p>The binary format stores JASS primitive globals (integer, real, boolean,
 * string) and per-player resource totals so that mission progress survives a
 * crash or intentional quit.  Complex handles (units, triggers, …) are not
 * serialised; those require a full live-game restore which is done separately
 * via the gamecache hero carry-over system.</p>
 *
 * <h3>File layout</h3>
 * <pre>
 *   magic    (int)   0x57335331  "W3S1"
 *   version  (int)   1
 *   mapPath  (UTF)   path passed to SaveGame
 *   nGlobals (int)
 *   for each global:
 *     name   (UTF)
 *     type   (byte)  0=int 1=real 2=bool 3=string
 *     value  (varies)
 *   nPlayers (int)
 *   for each player:
 *     gold   (int)
 *     lumber (int)
 * </pre>
 */
public final class CGameSave {
	private static final int FILE_MAGIC = 0x57335331; // "W3S1"
	private static final int FILE_VERSION = 1;

	private static final byte TYPE_INT = 0;
	private static final byte TYPE_REAL = 1;
	private static final byte TYPE_BOOL = 2;
	private static final byte TYPE_STRING = 3;

	/** Saved JASS primitive globals: name → JassValue (int/real/bool/string). */
	public final Map<String, JassValue> globals;

	/** Per-player gold totals (indexed by player slot). */
	public final int[] gold;

	/** Per-player lumber totals (indexed by player slot). */
	public final int[] lumber;

	/** Map path as supplied to SaveGame. */
	public final String mapPath;

	public CGameSave(final String mapPath, final int numPlayers) {
		this.mapPath = mapPath;
		this.gold = new int[numPlayers];
		this.lumber = new int[numPlayers];
		this.globals = new HashMap<>();
	}

	private CGameSave(final String mapPath, final int[] gold, final int[] lumber,
			final Map<String, JassValue> globals) {
		this.mapPath = mapPath;
		this.gold = gold;
		this.lumber = lumber;
		this.globals = globals;
	}

	// -----------------------------------------------------------------------
	// Collection helpers
	// -----------------------------------------------------------------------

	/**
	 * Populates {@link #globals} with all primitive (non-array, non-handle) JASS
	 * globals from {@code globalScope}.
	 */
	public void collectGlobals(final GlobalScope globalScope) {
		this.globals.clear();
		for (final Map.Entry<String, GlobalScopeAssignable> entry : globalScope.getAllGlobals().entrySet()) {
			final GlobalScopeAssignable assignable = entry.getValue();
			final JassValue value = assignable.getValue();
			if (value == null) {
				continue;
			}
			// Only persist primitive types; skip handles/arrays/code.
			if (isPrimitive(value)) {
				this.globals.put(entry.getKey(), value);
			}
		}
	}

	/**
	 * Restores primitive globals from this save into {@code globalScope}.
	 * Globals that no longer exist in the scope are silently skipped.
	 */
	public void restoreGlobals(final GlobalScope globalScope) {
		for (final Map.Entry<String, JassValue> entry : this.globals.entrySet()) {
			try {
				final GlobalScopeAssignable assignable = globalScope.getAssignableGlobal(entry.getKey());
				if (assignable != null) {
					assignable.setValue(entry.getValue());
				}
			}
			catch (final Exception e) {
				System.err.println("CGameSave: could not restore global '" + entry.getKey() + "': " + e.getMessage());
			}
		}
	}

	// -----------------------------------------------------------------------
	// Persistence
	// -----------------------------------------------------------------------

	/**
	 * Writes this save to {@code file}, creating parent directories as needed.
	 *
	 * @throws IOException on I/O failure
	 */
	public void save(final File file) throws IOException {
		final File dir = file.getParentFile();
		if (dir != null && !dir.exists()) {
			dir.mkdirs();
		}
		try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
			out.writeInt(FILE_MAGIC);
			out.writeInt(FILE_VERSION);
			out.writeUTF(this.mapPath != null ? this.mapPath : "");

			// Globals
			out.writeInt(this.globals.size());
			for (final Map.Entry<String, JassValue> entry : this.globals.entrySet()) {
				out.writeUTF(entry.getKey());
				writeValue(out, entry.getValue());
			}

			// Player resources
			out.writeInt(this.gold.length);
			for (int i = 0; i < this.gold.length; i++) {
				out.writeInt(this.gold[i]);
				out.writeInt(this.lumber[i]);
			}
		}
	}

	/**
	 * Loads a save from {@code file}.
	 *
	 * @return the loaded save, or {@code null} if the file is missing, corrupt, or
	 *         uses an unrecognised format.
	 */
	public static CGameSave tryLoad(final File file) {
		if (!file.exists()) {
			return null;
		}
		try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
			final int magic = in.readInt();
			if (magic != FILE_MAGIC) {
				System.err.println("CGameSave: bad magic in " + file + " (expected " + Integer.toHexString(FILE_MAGIC)
						+ ", got " + Integer.toHexString(magic) + ")");
				return null;
			}
			final int version = in.readInt();
			if (version != FILE_VERSION) {
				System.err.println("CGameSave: unsupported version " + version + " in " + file);
				return null;
			}
			final String mapPath = in.readUTF();

			final int nGlobals = in.readInt();
			final Map<String, JassValue> globals = new HashMap<>(nGlobals * 2);
			for (int i = 0; i < nGlobals; i++) {
				final String name = in.readUTF();
				final JassValue value = readValue(in);
				if (value != null) {
					globals.put(name, value);
				}
			}

			final int nPlayers = in.readInt();
			final int[] gold = new int[nPlayers];
			final int[] lumber = new int[nPlayers];
			for (int i = 0; i < nPlayers; i++) {
				gold[i] = in.readInt();
				lumber[i] = in.readInt();
			}

			return new CGameSave(mapPath, gold, lumber, globals);
		}
		catch (final IOException e) {
			System.err.println("CGameSave: failed to load " + file + ": " + e.getMessage());
			return null;
		}
	}

	// -----------------------------------------------------------------------
	// Private helpers
	// -----------------------------------------------------------------------

	private static boolean isPrimitive(final JassValue value) {
		return (value instanceof IntegerJassValue)
				|| (value instanceof RealJassValue)
				|| (value instanceof BooleanJassValue)
				|| (value instanceof StringJassValue);
	}

	private static void writeValue(final DataOutputStream out, final JassValue value) throws IOException {
		if (value instanceof IntegerJassValue) {
			out.writeByte(TYPE_INT);
			out.writeInt(((IntegerJassValue) value).getValue());
		}
		else if (value instanceof RealJassValue) {
			out.writeByte(TYPE_REAL);
			out.writeDouble(((RealJassValue) value).getValue());
		}
		else if (value instanceof BooleanJassValue) {
			out.writeByte(TYPE_BOOL);
			final Boolean b = value.visit(BooleanJassValueVisitor.getInstance());
			out.writeBoolean(b != null && b);
		}
		else {
			final String s = value.visit(StringJassValueVisitor.getInstance());
			out.writeByte(TYPE_STRING);
			out.writeUTF(s != null ? s : "");
		}
	}

	private static JassValue readValue(final DataInputStream in) throws IOException {
		final byte type = in.readByte();
		switch (type) {
		case TYPE_INT:
			return IntegerJassValue.of(in.readInt());
		case TYPE_REAL:
			return RealJassValue.of(in.readDouble());
		case TYPE_BOOL:
			return BooleanJassValue.of(in.readBoolean());
		case TYPE_STRING:
			return StringJassValue.of(in.readUTF());
		default:
			System.err.println("CGameSave: unknown value type " + type + " — skipping remaining globals");
			return null;
		}
	}
}
