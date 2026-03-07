package com.etheller.warsmash.viewer5.handlers.w3x.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.etheller.interpreter.ast.value.BooleanJassValue;
import com.etheller.interpreter.ast.value.IntegerJassValue;
import com.etheller.interpreter.ast.value.JassValue;
import com.etheller.interpreter.ast.value.RealJassValue;
import com.etheller.interpreter.ast.value.StringJassValue;

class CGameSaveTest {

	@TempDir
	File tmpDir;

	// ---- roundtrip ----

	@Test
	void roundtripIntegerGlobal() throws IOException {
		final CGameSave save = new CGameSave("mission01", 2);
		save.globals.put("udg_KillCount", IntegerJassValue.of(42));
		final File f = new File(this.tmpDir, "mission01.w3s");
		save.save(f);

		final CGameSave loaded = CGameSave.tryLoad(f);
		assertNotNull(loaded);
		final JassValue v = loaded.globals.get("udg_KillCount");
		assertNotNull(v);
		assertTrue(v instanceof IntegerJassValue);
		assertEquals(42, ((IntegerJassValue) v).getValue());
	}

	@Test
	void roundtripRealGlobal() throws IOException {
		final CGameSave save = new CGameSave("mission01", 2);
		save.globals.put("udg_Timer", RealJassValue.of(3.14));
		final File f = new File(this.tmpDir, "mission01.w3s");
		save.save(f);

		final CGameSave loaded = CGameSave.tryLoad(f);
		assertNotNull(loaded);
		final JassValue v = loaded.globals.get("udg_Timer");
		assertNotNull(v);
		assertTrue(v instanceof RealJassValue);
		assertEquals(3.14, ((RealJassValue) v).getValue(), 1e-9);
	}

	@Test
	void roundtripBooleanGlobal() throws IOException {
		final CGameSave save = new CGameSave("mission01", 2);
		save.globals.put("udg_QuestDone", BooleanJassValue.TRUE);
		final File f = new File(this.tmpDir, "mission01.w3s");
		save.save(f);

		final CGameSave loaded = CGameSave.tryLoad(f);
		assertNotNull(loaded);
		final JassValue v = loaded.globals.get("udg_QuestDone");
		assertNotNull(v);
		assertTrue(v instanceof BooleanJassValue);
		assertEquals(BooleanJassValue.TRUE, v);
	}

	@Test
	void roundtripStringGlobal() throws IOException {
		final CGameSave save = new CGameSave("mission01", 2);
		save.globals.put("udg_HeroName", StringJassValue.of("Arthas"));
		final File f = new File(this.tmpDir, "mission01.w3s");
		save.save(f);

		final CGameSave loaded = CGameSave.tryLoad(f);
		assertNotNull(loaded);
		final JassValue v = loaded.globals.get("udg_HeroName");
		assertNotNull(v);
		assertTrue(v instanceof StringJassValue);
		assertEquals("Arthas", ((StringJassValue) v).getValue());
	}

	@Test
	void roundtripPlayerResources() throws IOException {
		final CGameSave save = new CGameSave("mission01", 4);
		save.gold[0] = 500;
		save.lumber[0] = 200;
		save.gold[1] = 0;
		save.lumber[1] = 0;
		final File f = new File(this.tmpDir, "mission01.w3s");
		save.save(f);

		final CGameSave loaded = CGameSave.tryLoad(f);
		assertNotNull(loaded);
		assertEquals(4, loaded.gold.length);
		assertEquals(500, loaded.gold[0]);
		assertEquals(200, loaded.lumber[0]);
		assertEquals(0, loaded.gold[1]);
	}

	@Test
	void roundtripMapPath() throws IOException {
		final CGameSave save = new CGameSave("Human\\Human01", 1);
		final File f = new File(this.tmpDir, "Human01.w3s");
		save.save(f);

		final CGameSave loaded = CGameSave.tryLoad(f);
		assertNotNull(loaded);
		assertEquals("Human\\Human01", loaded.mapPath);
	}

	@Test
	void roundtripMultipleGlobals() throws IOException {
		final CGameSave save = new CGameSave("mission01", 2);
		save.globals.put("udg_A", IntegerJassValue.of(1));
		save.globals.put("udg_B", RealJassValue.of(2.5));
		save.globals.put("udg_C", BooleanJassValue.FALSE);
		save.globals.put("udg_D", StringJassValue.of("hello"));
		final File f = new File(this.tmpDir, "mission01.w3s");
		save.save(f);

		final CGameSave loaded = CGameSave.tryLoad(f);
		assertNotNull(loaded);
		assertEquals(4, loaded.globals.size());
		assertEquals(1, ((IntegerJassValue) loaded.globals.get("udg_A")).getValue());
		assertEquals(2.5, ((RealJassValue) loaded.globals.get("udg_B")).getValue(), 1e-9);
		assertEquals(BooleanJassValue.FALSE, loaded.globals.get("udg_C"));
		assertEquals("hello", ((StringJassValue) loaded.globals.get("udg_D")).getValue());
	}

	// ---- error handling ----

	@Test
	void tryLoadReturnsNullForMissingFile() {
		final File f = new File(this.tmpDir, "nonexistent.w3s");
		assertNull(CGameSave.tryLoad(f));
	}

	@Test
	void tryLoadReturnsNullForCorruptFile() throws IOException {
		final File f = new File(this.tmpDir, "corrupt.w3s");
		java.nio.file.Files.write(f.toPath(), new byte[]{0x00, 0x01, 0x02, 0x03});
		assertNull(CGameSave.tryLoad(f));
	}

	@Test
	void saveDirCreatedIfMissing() throws IOException {
		final File subDir = new File(this.tmpDir, "nested" + File.separator + "saves");
		final CGameSave save = new CGameSave("test", 1);
		final File f = new File(subDir, "test.w3s");
		save.save(f);
		assertTrue(f.exists());
	}
}
