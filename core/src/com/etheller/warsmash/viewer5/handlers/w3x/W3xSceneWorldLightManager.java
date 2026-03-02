package com.etheller.warsmash.viewer5.handlers.w3x;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import com.etheller.warsmash.viewer5.SceneLightInstance;
import com.etheller.warsmash.viewer5.SceneLightManager;
import com.etheller.warsmash.viewer5.gl.DataTexture;
import com.etheller.warsmash.viewer5.handlers.mdx.LightInstance;

public class W3xSceneWorldLightManager implements SceneLightManager, W3xSceneLightManager {
	private static final int LIGHT_REPORT_INTERVAL = 3600; // ~60 s at 60 fps

	public final List<LightInstance> lights;
	/** Shared buffer used to pack unit light data for GPU upload. */
	private FloatBuffer unitLightBuffer;
	/** Separate buffer for terrain light data (different DNC slot 0). */
	private FloatBuffer terrainLightBuffer;
	private final DataTexture unitLightsTexture;
	private final DataTexture terrainLightsTexture;
	private final War3MapViewer viewer;
	private int terrainLightCount;
	private int unitLightCount;
	private int updateTick = 0;

	public W3xSceneWorldLightManager(final War3MapViewer viewer) {
		this.viewer = viewer;
		this.lights = new ArrayList<>();
		this.unitLightsTexture = new DataTexture(viewer.gl, 4, 4, 1);
		this.terrainLightsTexture = new DataTexture(viewer.gl, 4, 4, 1);
		final int initialFloats = 16; // one light slot
		this.unitLightBuffer = ByteBuffer.allocateDirect(initialFloats * 4).order(ByteOrder.nativeOrder())
				.asFloatBuffer();
		this.terrainLightBuffer = ByteBuffer.allocateDirect(initialFloats * 4).order(ByteOrder.nativeOrder())
				.asFloatBuffer();
	}

	@Override
	public void add(final SceneLightInstance lightInstance) {
		// TODO redesign to avoid cast
		final LightInstance mdxLight = (LightInstance) lightInstance;
		this.lights.add(mdxLight);
	}

	@Override
	public void remove(final SceneLightInstance lightInstance) {
		// TODO redesign to avoid cast
		final LightInstance mdxLight = (LightInstance) lightInstance;
		// ArrayList.remove() is a no-op if the element is absent, so this is idempotent.
		this.lights.remove(mdxLight);
	}

	@Override
	public void update() {
		this.updateTick++;
		if (this.updateTick >= LIGHT_REPORT_INTERVAL) {
			System.out.printf("[LightManager] active dynamic lights=%d%n", this.lights.size());
			this.updateTick = 0;
		}

		// Advance generation once per frame so LightInstance.bind() recomputes
		// keyframe data at most once per light per frame, regardless of how many
		// GPU textures the light is written into.
		LightInstance.advanceGeneration();

		final int numberOfLights = this.lights.size() + 1;
		final int floatsNeeded = numberOfLights * 16;
		if (floatsNeeded > this.unitLightBuffer.capacity()) {
			this.unitLightBuffer = ByteBuffer.allocateDirect(floatsNeeded * 4).order(ByteOrder.nativeOrder())
					.asFloatBuffer();
			this.terrainLightBuffer = ByteBuffer.allocateDirect(floatsNeeded * 4).order(ByteOrder.nativeOrder())
					.asFloatBuffer();
			this.unitLightsTexture.reserve(4, numberOfLights);
			this.terrainLightsTexture.reserve(4, numberOfLights);
		}

		// Pack unit texture: [dncUnit slot] + [point lights]
		this.unitLightBuffer.clear();
		this.unitLightCount = 0;
		int unitOffset = 0;
		if (this.viewer.dncUnit != null && !this.viewer.dncUnit.lights.isEmpty()) {
			this.viewer.dncUnit.lights.get(0).bind(0, this.unitLightBuffer);
			unitOffset += 16;
			this.unitLightCount++;
		}

		// Pack terrain texture: [dncTerrain slot] + [point lights]
		this.terrainLightBuffer.clear();
		this.terrainLightCount = 0;
		int terrainOffset = 0;
		if (this.viewer.dncTerrain != null && !this.viewer.dncTerrain.lights.isEmpty()) {
			this.viewer.dncTerrain.lights.get(0).bind(0, this.terrainLightBuffer);
			terrainOffset += 16;
			this.terrainLightCount++;
		}

		// Single pass over point lights. Each light recomputes its keyframe data at
		// most once per generation (the first bind() call rebuilds the cache; the
		// second bind() call bulk-copies from that cache).
		for (final LightInstance light : this.lights) {
			light.bind(unitOffset, this.unitLightBuffer);
			unitOffset += 16;
			this.unitLightCount++;

			light.bind(terrainOffset, this.terrainLightBuffer);
			terrainOffset += 16;
			this.terrainLightCount++;
		}

		this.unitLightBuffer.limit(unitOffset);
		this.unitLightsTexture.bindAndUpdate(this.unitLightBuffer, 4, this.unitLightCount);

		this.terrainLightBuffer.limit(terrainOffset);
		this.terrainLightsTexture.bindAndUpdate(this.terrainLightBuffer, 4, this.terrainLightCount);
	}

	@Override
	public DataTexture getUnitLightsTexture() {
		return this.unitLightsTexture;
	}

	@Override
	public int getUnitLightCount() {
		return this.unitLightCount;
	}

	@Override
	public DataTexture getTerrainLightsTexture() {
		return this.terrainLightsTexture;
	}

	@Override
	public int getTerrainLightCount() {
		return this.terrainLightCount;
	}
}
