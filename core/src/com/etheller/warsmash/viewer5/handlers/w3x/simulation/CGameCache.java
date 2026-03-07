package com.etheller.warsmash.viewer5.handlers.w3x.simulation;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.etheller.interpreter.ast.util.CHandle;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.StoredUnitData.StoredItemData;

/**
 * In-memory gamecache for campaign hero carry-over and inter-map persistent
 * state. Mirrors the WC3 gamecache handle behaviour: values are keyed by
 * (missionKey, key) string pairs and stored per type (integer, real, boolean,
 * string, unit).
 *
 * <p>Disk persistence is provided via {@link #save(File)} and
 * {@link #tryLoadFromFile(File, String)}.  The binary format uses a custom
 * magic/version header so stale or corrupt files are silently ignored.</p>
 */
public final class CGameCache implements CHandle {
	private static final int FILE_MAGIC = 0x57335630; // "W3V0"
	private static final int FILE_VERSION = 1;

	private static final AtomicInteger HANDLE_ID_COUNTER = new AtomicInteger(0);

	private final String name;
	private final int handleId;
	private final Map<String, Map<String, Integer>> integers = new HashMap<>();
	private final Map<String, Map<String, Float>> reals = new HashMap<>();
	private final Map<String, Map<String, Boolean>> booleans = new HashMap<>();
	private final Map<String, Map<String, String>> strings = new HashMap<>();
	private final Map<String, Map<String, StoredUnitData>> units = new HashMap<>();

	public CGameCache(final String name) {
		this.name = name;
		this.handleId = HANDLE_ID_COUNTER.incrementAndGet();
	}

	@Override
	public int getHandleId() {
		return this.handleId;
	}

	public String getName() {
		return this.name;
	}

	// ---- integer ----

	public void storeInteger(final String missionKey, final String key, final int value) {
		getOrCreate(this.integers, missionKey).put(key, value);
	}

	public int getStoredInteger(final String missionKey, final String key) {
		final Map<String, Integer> mission = this.integers.get(missionKey);
		if (mission == null) {
			return 0;
		}
		final Integer v = mission.get(key);
		return v != null ? v : 0;
	}

	public boolean haveStoredInteger(final String missionKey, final String key) {
		final Map<String, Integer> mission = this.integers.get(missionKey);
		return mission != null && mission.containsKey(key);
	}

	public void flushStoredInteger(final String missionKey, final String key) {
		final Map<String, Integer> mission = this.integers.get(missionKey);
		if (mission != null) {
			mission.remove(key);
		}
	}

	// ---- real ----

	public void storeReal(final String missionKey, final String key, final float value) {
		getOrCreate(this.reals, missionKey).put(key, value);
	}

	public float getStoredReal(final String missionKey, final String key) {
		final Map<String, Float> mission = this.reals.get(missionKey);
		if (mission == null) {
			return 0f;
		}
		final Float v = mission.get(key);
		return v != null ? v : 0f;
	}

	public boolean haveStoredReal(final String missionKey, final String key) {
		final Map<String, Float> mission = this.reals.get(missionKey);
		return mission != null && mission.containsKey(key);
	}

	public void flushStoredReal(final String missionKey, final String key) {
		final Map<String, Float> mission = this.reals.get(missionKey);
		if (mission != null) {
			mission.remove(key);
		}
	}

	// ---- boolean ----

	public void storeBoolean(final String missionKey, final String key, final boolean value) {
		getOrCreate(this.booleans, missionKey).put(key, value);
	}

	public boolean getStoredBoolean(final String missionKey, final String key) {
		final Map<String, Boolean> mission = this.booleans.get(missionKey);
		if (mission == null) {
			return false;
		}
		final Boolean v = mission.get(key);
		return v != null ? v : false;
	}

	public boolean haveStoredBoolean(final String missionKey, final String key) {
		final Map<String, Boolean> mission = this.booleans.get(missionKey);
		return mission != null && mission.containsKey(key);
	}

	public void flushStoredBoolean(final String missionKey, final String key) {
		final Map<String, Boolean> mission = this.booleans.get(missionKey);
		if (mission != null) {
			mission.remove(key);
		}
	}

	// ---- string ----

	public void storeString(final String missionKey, final String key, final String value) {
		getOrCreate(this.strings, missionKey).put(key, value != null ? value : "");
	}

	public String getStoredString(final String missionKey, final String key) {
		final Map<String, String> mission = this.strings.get(missionKey);
		if (mission == null) {
			return "";
		}
		final String v = mission.get(key);
		return v != null ? v : "";
	}

	public boolean haveStoredString(final String missionKey, final String key) {
		final Map<String, String> mission = this.strings.get(missionKey);
		return mission != null && mission.containsKey(key);
	}

	public void flushStoredString(final String missionKey, final String key) {
		final Map<String, String> mission = this.strings.get(missionKey);
		if (mission != null) {
			mission.remove(key);
		}
	}

	// ---- mission flush ----

	/**
	 * Removes all values for a given missionKey across all types including units.
	 */
	public void flushStoredMission(final String missionKey) {
		this.integers.remove(missionKey);
		this.reals.remove(missionKey);
		this.booleans.remove(missionKey);
		this.strings.remove(missionKey);
		this.units.remove(missionKey);
	}

	/**
	 * Removes all values across all mission keys and types.
	 */
	public void flushAll() {
		this.integers.clear();
		this.reals.clear();
		this.booleans.clear();
		this.strings.clear();
		this.units.clear();
	}

	// ---- unit ----

	public void storeUnit(final String missionKey, final String key, final StoredUnitData data) {
		getOrCreate(this.units, missionKey).put(key, data);
	}

	public StoredUnitData getStoredUnit(final String missionKey, final String key) {
		final Map<String, StoredUnitData> mission = this.units.get(missionKey);
		if (mission == null) {
			return null;
		}
		return mission.get(key);
	}

	public boolean haveStoredUnit(final String missionKey, final String key) {
		final Map<String, StoredUnitData> mission = this.units.get(missionKey);
		return mission != null && mission.containsKey(key);
	}

	public void flushStoredUnit(final String missionKey, final String key) {
		final Map<String, StoredUnitData> mission = this.units.get(missionKey);
		if (mission != null) {
			mission.remove(key);
		}
	}

	// ---- disk persistence ----

	/**
	 * Saves this gamecache to {@code file} using a compact binary format.
	 * Creates parent directories as needed.
	 *
	 * @param file target file; overwritten if it already exists
	 * @throws IOException on any I/O error
	 */
	public void save(final File file) throws IOException {
		final File parent = file.getParentFile();
		if (parent != null) {
			parent.mkdirs();
		}
		try (final DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
			out.writeInt(FILE_MAGIC);
			out.writeInt(FILE_VERSION);

			// integers
			final List<int[]> intEntries = flattenTyped(this.integers);
			final List<String[]> intKeys = flattenKeys(this.integers);
			out.writeInt(intKeys.size());
			for (int i = 0; i < intKeys.size(); i++) {
				out.writeUTF(intKeys.get(i)[0]);
				out.writeUTF(intKeys.get(i)[1]);
				out.writeInt(intEntries.get(i)[0]);
			}

			// reals
			final List<float[]> realEntries = flattenTypedFloat(this.reals);
			final List<String[]> realKeys = flattenKeys(this.reals);
			out.writeInt(realKeys.size());
			for (int i = 0; i < realKeys.size(); i++) {
				out.writeUTF(realKeys.get(i)[0]);
				out.writeUTF(realKeys.get(i)[1]);
				out.writeFloat(realEntries.get(i)[0]);
			}

			// booleans
			final List<boolean[]> boolEntries = flattenTypedBool(this.booleans);
			final List<String[]> boolKeys = flattenKeys(this.booleans);
			out.writeInt(boolKeys.size());
			for (int i = 0; i < boolKeys.size(); i++) {
				out.writeUTF(boolKeys.get(i)[0]);
				out.writeUTF(boolKeys.get(i)[1]);
				out.writeBoolean(boolEntries.get(i)[0]);
			}

			// strings
			final List<String[]> strEntries = new ArrayList<>();
			final List<String[]> strKeys = new ArrayList<>();
			for (final Map.Entry<String, Map<String, String>> missionEntry : this.strings.entrySet()) {
				for (final Map.Entry<String, String> entry : missionEntry.getValue().entrySet()) {
					strKeys.add(new String[] { missionEntry.getKey(), entry.getKey() });
					strEntries.add(new String[] { entry.getValue() });
				}
			}
			out.writeInt(strKeys.size());
			for (int i = 0; i < strKeys.size(); i++) {
				out.writeUTF(strKeys.get(i)[0]);
				out.writeUTF(strKeys.get(i)[1]);
				out.writeUTF(strEntries.get(i)[0]);
			}

			// units
			final List<String[]> unitKeys = flattenKeys(this.units);
			final List<StoredUnitData> unitEntries = new ArrayList<>();
			for (final Map.Entry<String, Map<String, StoredUnitData>> missionEntry : this.units.entrySet()) {
				for (final StoredUnitData data : missionEntry.getValue().values()) {
					unitEntries.add(data);
				}
			}
			out.writeInt(unitKeys.size());
			for (int i = 0; i < unitKeys.size(); i++) {
				final StoredUnitData data = unitEntries.get(i);
				out.writeUTF(unitKeys.get(i)[0]);
				out.writeUTF(unitKeys.get(i)[1]);
				out.writeInt(data.unitTypeId.getValue());
				out.writeInt(data.xp);
				out.writeInt(data.skillPoints);
				out.writeInt(data.strengthBase);
				out.writeInt(data.agilityBase);
				out.writeInt(data.intelligenceBase);
				out.writeInt(data.strengthBonus);
				out.writeInt(data.agilityBonus);
				out.writeInt(data.intelligenceBonus);
				out.writeUTF(data.properName);
				final int itemCount = countNonNullItems(data.items);
				out.writeInt(itemCount);
				if (data.items != null) {
					for (int slot = 0; slot < data.items.length; slot++) {
						final StoredItemData item = data.items[slot];
						if (item != null) {
							out.writeInt(slot);
							out.writeInt(item.typeId.getValue());
							out.writeInt(item.charges);
						}
					}
				}
			}
		}
	}

	/**
	 * Attempts to load a gamecache from {@code file}.  Returns {@code null}
	 * silently if the file does not exist, is empty, or has an unrecognised
	 * format, so callers always fall back to an empty cache rather than crashing.
	 *
	 * @param file      source file
	 * @param cacheName name for the returned {@link CGameCache} instance
	 * @return populated cache, or {@code null} on any load failure
	 */
	public static CGameCache tryLoadFromFile(final File file, final String cacheName) {
		if (!file.exists() || file.length() == 0) {
			return null;
		}
		try (final DataInputStream in = new DataInputStream(new FileInputStream(file))) {
			final int magic = in.readInt();
			final int version = in.readInt();
			if (magic != FILE_MAGIC || version != FILE_VERSION) {
				return null;
			}
			final CGameCache cache = new CGameCache(cacheName);

			// integers
			final int intCount = in.readInt();
			for (int i = 0; i < intCount; i++) {
				final String mk = in.readUTF();
				final String k = in.readUTF();
				final int v = in.readInt();
				cache.storeInteger(mk, k, v);
			}

			// reals
			final int realCount = in.readInt();
			for (int i = 0; i < realCount; i++) {
				final String mk = in.readUTF();
				final String k = in.readUTF();
				final float v = in.readFloat();
				cache.storeReal(mk, k, v);
			}

			// booleans
			final int boolCount = in.readInt();
			for (int i = 0; i < boolCount; i++) {
				final String mk = in.readUTF();
				final String k = in.readUTF();
				final boolean v = in.readBoolean();
				cache.storeBoolean(mk, k, v);
			}

			// strings
			final int strCount = in.readInt();
			for (int i = 0; i < strCount; i++) {
				final String mk = in.readUTF();
				final String k = in.readUTF();
				final String v = in.readUTF();
				cache.storeString(mk, k, v);
			}

			// units
			final int unitCount = in.readInt();
			for (int i = 0; i < unitCount; i++) {
				final String mk = in.readUTF();
				final String k = in.readUTF();
				final War3ID typeId = new War3ID(in.readInt());
				final int xp = in.readInt();
				final int skillPoints = in.readInt();
				final int strBase = in.readInt();
				final int agiBase = in.readInt();
				final int intBase = in.readInt();
				final int strBonus = in.readInt();
				final int agiBonus = in.readInt();
				final int intBonus = in.readInt();
				final String properName = in.readUTF();
				final int itemCount = in.readInt();
				StoredItemData[] items = null;
				if (itemCount > 0) {
					items = new StoredItemData[6];
					for (int s = 0; s < itemCount; s++) {
						final int slot = in.readInt();
						final War3ID itemTypeId = new War3ID(in.readInt());
						final int charges = in.readInt();
						if (slot >= 0 && slot < items.length) {
							items[slot] = new StoredItemData(itemTypeId, charges);
						}
					}
				}
				cache.storeUnit(mk, k, new StoredUnitData(typeId, xp, skillPoints, strBase, agiBase, intBase,
						strBonus, agiBonus, intBonus, properName, items));
			}
			return cache;
		}
		catch (final IOException e) {
			System.err.println("CGameCache.tryLoadFromFile: failed to read " + file + ": " + e.getMessage());
			return null;
		}
	}

	// ---- helpers ----

	private static <V> Map<String, V> getOrCreate(final Map<String, Map<String, V>> parent, final String key) {
		Map<String, V> child = parent.get(key);
		if (child == null) {
			child = new HashMap<>();
			parent.put(key, child);
		}
		return child;
	}

	private static <V> List<String[]> flattenKeys(final Map<String, Map<String, V>> outer) {
		final List<String[]> result = new ArrayList<>();
		for (final Map.Entry<String, Map<String, V>> missionEntry : outer.entrySet()) {
			for (final String key : missionEntry.getValue().keySet()) {
				result.add(new String[] { missionEntry.getKey(), key });
			}
		}
		return result;
	}

	private static List<int[]> flattenTyped(final Map<String, Map<String, Integer>> outer) {
		final List<int[]> result = new ArrayList<>();
		for (final Map.Entry<String, Map<String, Integer>> missionEntry : outer.entrySet()) {
			for (final Integer v : missionEntry.getValue().values()) {
				result.add(new int[] { v });
			}
		}
		return result;
	}

	private static List<float[]> flattenTypedFloat(final Map<String, Map<String, Float>> outer) {
		final List<float[]> result = new ArrayList<>();
		for (final Map.Entry<String, Map<String, Float>> missionEntry : outer.entrySet()) {
			for (final Float v : missionEntry.getValue().values()) {
				result.add(new float[] { v });
			}
		}
		return result;
	}

	private static List<boolean[]> flattenTypedBool(final Map<String, Map<String, Boolean>> outer) {
		final List<boolean[]> result = new ArrayList<>();
		for (final Map.Entry<String, Map<String, Boolean>> missionEntry : outer.entrySet()) {
			for (final Boolean v : missionEntry.getValue().values()) {
				result.add(new boolean[] { v });
			}
		}
		return result;
	}

	private static int countNonNullItems(final StoredItemData[] items) {
		if (items == null) {
			return 0;
		}
		int count = 0;
		for (final StoredItemData item : items) {
			if (item != null) {
				count++;
			}
		}
		return count;
	}
}
