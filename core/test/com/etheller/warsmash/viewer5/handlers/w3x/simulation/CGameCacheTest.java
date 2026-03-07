package com.etheller.warsmash.viewer5.handlers.w3x.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.StoredUnitData.StoredItemData;

class CGameCacheTest {

	private CGameCache cache;

	@BeforeEach
	void setUp() {
		this.cache = new CGameCache("testcache.w3v");
	}

	// ---- handle identity ----

	@Test
	void handleIdIsPositive() {
		assertTrue(this.cache.getHandleId() > 0);
	}

	@Test
	void distinctCachesHaveDistinctHandleIds() {
		final CGameCache other = new CGameCache("other.w3v");
		assertNotEquals(this.cache.getHandleId(), other.getHandleId());
	}

	@Test
	void cacheNameIsPreserved() {
		assertEquals("testcache.w3v", this.cache.getName());
	}

	// ---- integer ----

	@Test
	void storedIntegerIsRetrievedCorrectly() {
		this.cache.storeInteger("heroes", "arthas_level", 5);
		assertEquals(5, this.cache.getStoredInteger("heroes", "arthas_level"));
	}

	@Test
	void missingIntegerReturnsZero() {
		assertEquals(0, this.cache.getStoredInteger("heroes", "nonexistent"));
	}

	@Test
	void haveStoredIntegerReturnsTrueAfterStore() {
		this.cache.storeInteger("heroes", "arthas_level", 3);
		assertTrue(this.cache.haveStoredInteger("heroes", "arthas_level"));
	}

	@Test
	void haveStoredIntegerReturnsFalseBeforeStore() {
		assertFalse(this.cache.haveStoredInteger("heroes", "not_stored"));
	}

	@Test
	void flushStoredIntegerRemovesValue() {
		this.cache.storeInteger("heroes", "arthas_level", 5);
		this.cache.flushStoredInteger("heroes", "arthas_level");
		assertFalse(this.cache.haveStoredInteger("heroes", "arthas_level"));
		assertEquals(0, this.cache.getStoredInteger("heroes", "arthas_level"));
	}

	// ---- real ----

	@Test
	void storedRealIsRetrievedCorrectly() {
		this.cache.storeReal("heroes", "hp_pct", 0.75f);
		assertEquals(0.75f, this.cache.getStoredReal("heroes", "hp_pct"), 1e-6f);
	}

	@Test
	void missingRealReturnsZero() {
		assertEquals(0f, this.cache.getStoredReal("heroes", "nonexistent"), 1e-6f);
	}

	@Test
	void haveStoredRealReturnsTrueAfterStore() {
		this.cache.storeReal("heroes", "hp_pct", 1.0f);
		assertTrue(this.cache.haveStoredReal("heroes", "hp_pct"));
	}

	@Test
	void flushStoredRealRemovesValue() {
		this.cache.storeReal("heroes", "hp_pct", 0.5f);
		this.cache.flushStoredReal("heroes", "hp_pct");
		assertFalse(this.cache.haveStoredReal("heroes", "hp_pct"));
	}

	// ---- boolean ----

	@Test
	void storedBooleanIsRetrievedCorrectly() {
		this.cache.storeBoolean("flags", "mission_complete", true);
		assertTrue(this.cache.getStoredBoolean("flags", "mission_complete"));
	}

	@Test
	void missingBooleanReturnsFalse() {
		assertFalse(this.cache.getStoredBoolean("flags", "nonexistent"));
	}

	@Test
	void haveStoredBooleanReturnsTrueAfterStore() {
		this.cache.storeBoolean("flags", "unlocked", false);
		assertTrue(this.cache.haveStoredBoolean("flags", "unlocked"));
	}

	@Test
	void flushStoredBooleanRemovesValue() {
		this.cache.storeBoolean("flags", "unlocked", true);
		this.cache.flushStoredBoolean("flags", "unlocked");
		assertFalse(this.cache.haveStoredBoolean("flags", "unlocked"));
	}

	// ---- string ----

	@Test
	void storedStringIsRetrievedCorrectly() {
		this.cache.storeString("heroes", "name", "Arthas");
		assertEquals("Arthas", this.cache.getStoredString("heroes", "name"));
	}

	@Test
	void missingStringReturnsEmpty() {
		assertEquals("", this.cache.getStoredString("heroes", "nonexistent"));
	}

	@Test
	void haveStoredStringReturnsTrueAfterStore() {
		this.cache.storeString("heroes", "name", "Arthas");
		assertTrue(this.cache.haveStoredString("heroes", "name"));
	}

	@Test
	void flushStoredStringRemovesValue() {
		this.cache.storeString("heroes", "name", "Arthas");
		this.cache.flushStoredString("heroes", "name");
		assertFalse(this.cache.haveStoredString("heroes", "name"));
	}

	// ---- mission flush ----

	@Test
	void flushStoredMissionRemovesAllTypesForMission() {
		this.cache.storeInteger("mission1", "level", 5);
		this.cache.storeReal("mission1", "hp", 0.9f);
		this.cache.storeBoolean("mission1", "done", true);
		this.cache.storeString("mission1", "hero", "Arthas");

		// mission2 data should survive
		this.cache.storeInteger("mission2", "level", 3);

		this.cache.flushStoredMission("mission1");

		assertFalse(this.cache.haveStoredInteger("mission1", "level"));
		assertFalse(this.cache.haveStoredReal("mission1", "hp"));
		assertFalse(this.cache.haveStoredBoolean("mission1", "done"));
		assertFalse(this.cache.haveStoredString("mission1", "hero"));

		// mission2 data is unaffected
		assertTrue(this.cache.haveStoredInteger("mission2", "level"));
	}

	// ---- flush all ----

	@Test
	void flushAllClearsEverything() {
		this.cache.storeInteger("m1", "k1", 1);
		this.cache.storeString("m2", "k2", "hello");

		this.cache.flushAll();

		assertFalse(this.cache.haveStoredInteger("m1", "k1"));
		assertFalse(this.cache.haveStoredString("m2", "k2"));
	}

	// ---- overwrite ----

	@Test
	void storedIntegerCanBeOverwritten() {
		this.cache.storeInteger("heroes", "level", 3);
		this.cache.storeInteger("heroes", "level", 7);
		assertEquals(7, this.cache.getStoredInteger("heroes", "level"));
	}

	// ---- multiple mission keys are independent ----

	@Test
	void differentMissionKeysDontInterfere() {
		this.cache.storeInteger("mission_a", "x", 10);
		this.cache.storeInteger("mission_b", "x", 20);

		assertEquals(10, this.cache.getStoredInteger("mission_a", "x"));
		assertEquals(20, this.cache.getStoredInteger("mission_b", "x"));
	}

	// ---- unit storage ----

	private static StoredUnitData heroSnapshot() {
		final StoredItemData[] items = new StoredItemData[6];
		items[0] = new StoredItemData(War3ID.fromString("ratf"), 0); // rat fink item
		items[2] = new StoredItemData(War3ID.fromString("stel"), 3); // storm earth lightning, 3 charges
		return new StoredUnitData(War3ID.fromString("Hpal"), 1500, 2, 22, 14, 18, 5, 0, 3, "Arthas", items);
	}

	@Test
	void storedUnitIsRetrievedCorrectly() {
		final StoredUnitData data = heroSnapshot();
		this.cache.storeUnit("Heroes", "Arthas", data);

		assertTrue(this.cache.haveStoredUnit("Heroes", "Arthas"));
		final StoredUnitData retrieved = this.cache.getStoredUnit("Heroes", "Arthas");
		assertNotNull(retrieved);
		assertEquals(War3ID.fromString("Hpal"), retrieved.unitTypeId);
		assertEquals(1500, retrieved.xp);
		assertEquals(2, retrieved.skillPoints);
		assertEquals(22, retrieved.strengthBase);
		assertEquals("Arthas", retrieved.properName);
		assertNotNull(retrieved.items);
		assertNotNull(retrieved.items[0]);
		assertEquals(War3ID.fromString("ratf"), retrieved.items[0].typeId);
		assertNull(retrieved.items[1]);
		assertNotNull(retrieved.items[2]);
		assertEquals(3, retrieved.items[2].charges);
	}

	@Test
	void missingUnitReturnsNull() {
		assertNull(this.cache.getStoredUnit("Heroes", "nobody"));
		assertFalse(this.cache.haveStoredUnit("Heroes", "nobody"));
	}

	@Test
	void flushStoredUnitRemovesUnit() {
		this.cache.storeUnit("Heroes", "Arthas", heroSnapshot());
		this.cache.flushStoredUnit("Heroes", "Arthas");

		assertFalse(this.cache.haveStoredUnit("Heroes", "Arthas"));
		assertNull(this.cache.getStoredUnit("Heroes", "Arthas"));
	}

	@Test
	void flushStoredMissionAlsoFlushesStoredUnits() {
		this.cache.storeUnit("m1", "hero", heroSnapshot());
		this.cache.storeInteger("m1", "gold", 200);

		this.cache.flushStoredMission("m1");

		assertFalse(this.cache.haveStoredUnit("m1", "hero"));
		assertFalse(this.cache.haveStoredInteger("m1", "gold"));
	}

	@Test
	void flushAllRemovesStoredUnits() {
		this.cache.storeUnit("m1", "hero", heroSnapshot());
		this.cache.flushAll();

		assertFalse(this.cache.haveStoredUnit("m1", "hero"));
	}

	// ---- disk persistence ----

	@Test
	void saveThenLoadRestoresAllTypes(@TempDir final File tmpDir) throws IOException {
		this.cache.storeInteger("missionA", "level", 7);
		this.cache.storeReal("missionA", "hp", 0.88f);
		this.cache.storeBoolean("missionA", "done", true);
		this.cache.storeString("missionA", "name", "Arthas");
		this.cache.storeUnit("Heroes", "Arthas", heroSnapshot());

		final File saveFile = new File(tmpDir, "testcache.w3v");
		this.cache.save(saveFile);

		final CGameCache loaded = CGameCache.tryLoadFromFile(saveFile, "testcache.w3v");
		assertNotNull(loaded);
		assertEquals(7, loaded.getStoredInteger("missionA", "level"));
		assertEquals(0.88f, loaded.getStoredReal("missionA", "hp"), 1e-5f);
		assertTrue(loaded.getStoredBoolean("missionA", "done"));
		assertEquals("Arthas", loaded.getStoredString("missionA", "name"));

		assertTrue(loaded.haveStoredUnit("Heroes", "Arthas"));
		final StoredUnitData unit = loaded.getStoredUnit("Heroes", "Arthas");
		assertEquals(War3ID.fromString("Hpal"), unit.unitTypeId);
		assertEquals(1500, unit.xp);
		assertEquals(2, unit.skillPoints);
		assertEquals(22, unit.strengthBase);
		assertEquals(14, unit.agilityBase);
		assertEquals(18, unit.intelligenceBase);
		assertEquals(5, unit.strengthBonus);
		assertEquals(0, unit.agilityBonus);
		assertEquals(3, unit.intelligenceBonus);
		assertEquals("Arthas", unit.properName);
		assertNotNull(unit.items);
		assertNotNull(unit.items[0]);
		assertEquals(War3ID.fromString("ratf"), unit.items[0].typeId);
		assertNull(unit.items[1]);
		assertNotNull(unit.items[2]);
		assertEquals(3, unit.items[2].charges);
	}

	@Test
	void tryLoadFromFileMissingFileReturnsNull(@TempDir final File tmpDir) {
		final File missing = new File(tmpDir, "noSuchFile.w3v");
		assertNull(CGameCache.tryLoadFromFile(missing, "noSuchFile.w3v"));
	}

	@Test
	void tryLoadFromFileCorruptFileReturnsNull(@TempDir final File tmpDir) throws IOException {
		final File corrupt = new File(tmpDir, "corrupt.w3v");
		Files.write(corrupt.toPath(), new byte[] { 0x00, 0x01, 0x02 });
		assertNull(CGameCache.tryLoadFromFile(corrupt, "corrupt.w3v"));
	}

	@Test
	void saveCreatesParentDirectories(@TempDir final File tmpDir) throws IOException {
		final File deep = new File(tmpDir, "a" + File.separator + "b" + File.separator + "cache.w3v");
		this.cache.storeString("k", "v", "hello");
		this.cache.save(deep);

		assertTrue(deep.exists());
		final CGameCache loaded = CGameCache.tryLoadFromFile(deep, "cache.w3v");
		assertNotNull(loaded);
		assertEquals("hello", loaded.getStoredString("k", "v"));
	}

	@Test
	void unitWithNoItemsRoundTrips(@TempDir final File tmpDir) throws IOException {
		final StoredUnitData noItems = new StoredUnitData(War3ID.fromString("Hamg"), 300, 1, 16, 12, 20, 0, 0, 0,
				"Jaina", null);
		this.cache.storeUnit("Heroes", "Jaina", noItems);

		final File saveFile = new File(tmpDir, "jaina.w3v");
		this.cache.save(saveFile);

		final CGameCache loaded = CGameCache.tryLoadFromFile(saveFile, "jaina.w3v");
		assertNotNull(loaded);
		final StoredUnitData unit = loaded.getStoredUnit("Heroes", "Jaina");
		assertNotNull(unit);
		assertEquals(War3ID.fromString("Hamg"), unit.unitTypeId);
		assertEquals("Jaina", unit.properName);
		assertEquals(300, unit.xp);
		assertNull(unit.items);
	}
}
