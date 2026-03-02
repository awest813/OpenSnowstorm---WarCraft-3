package com.etheller.warsmash.viewer5.handlers.mdx;

import java.nio.FloatBuffer;

import com.badlogic.gdx.math.Vector3;
import com.etheller.warsmash.viewer5.Scene;
import com.etheller.warsmash.viewer5.SceneLightInstance;
import com.etheller.warsmash.viewer5.UpdatableObject;

public class LightInstance implements UpdatableObject, SceneLightInstance {
	/**
	 * Global frame generation counter. Incremented once per frame by
	 * {@link #advanceGeneration()} so that each light's packed data is computed at
	 * most once per frame regardless of how many GPU textures it is written into.
	 */
	private static int currentGeneration = 0;

	private static final Vector3 vector3Heap = new Vector3();
	private static final float[] vectorHeap = new float[3];
	private static final float[] scalarHeap = new float[1];

	/** Packed 4×4 float block matching the GPU light-texture layout. */
	private final float[] cache = new float[16];
	/** Generation at which {@link #cache} was last filled. */
	private int cacheGeneration = -1;

	protected final MdxNode node;
	protected final Light light;
	private boolean visible;
	private boolean loadedInScene;
	private final MdxComplexInstance instance;

	public LightInstance(final MdxComplexInstance instance, final Light light) {
		this.instance = instance;
		this.node = instance.nodes[light.index];
		this.light = light;
	}

	/**
	 * Advance the global generation counter. Call once at the start of
	 * {@code W3xSceneWorldLightManager.update()} so that the per-frame cache is
	 * invalidated exactly once per render frame.
	 */
	public static void advanceGeneration() {
		currentGeneration++;
	}

	/**
	 * Fills {@code floatBuffer} starting at absolute position {@code offset} with
	 * the 16-float packed light block.
	 *
	 * <p>Data is computed from keyframe tracks at most once per generation (render
	 * frame). Subsequent calls within the same generation bulk-copy from the
	 * internal cache, avoiding redundant keyframe samples.
	 */
	@Override
	public void bind(final int offset, final FloatBuffer floatBuffer) {
		if (cacheGeneration != currentGeneration) {
			rebuildCache();
			cacheGeneration = currentGeneration;
		}
		floatBuffer.position(offset);
		floatBuffer.put(cache, 0, 16);
	}

	/**
	 * Recomputes all 16 floats of the light data and stores them in
	 * {@link #cache}.
	 */
	private void rebuildCache() {
		final int sequence = this.instance.sequence;
		final int frame = this.instance.frame;
		final int counter = this.instance.counter;

		this.light.getAttenuationStart(scalarHeap, sequence, frame, counter);
		final float attenuationStart = scalarHeap[0];
		this.light.getAttenuationEnd(scalarHeap, sequence, frame, counter);
		final float attenuationEnd = scalarHeap[0];
		this.light.getIntensity(scalarHeap, sequence, frame, counter);
		final float intensity = scalarHeap[0];
		this.light.getColor(vectorHeap, sequence, frame, counter);
		final float colorRed = vectorHeap[0];
		final float colorGreen = vectorHeap[1];
		final float colorBlue = vectorHeap[2];
		this.light.getAmbientIntensity(scalarHeap, sequence, frame, counter);
		final float ambientIntensity = scalarHeap[0];
		this.light.getAmbientColor(vectorHeap, sequence, frame, counter);
		final float ambientColorRed = vectorHeap[0];
		final float ambientColorGreen = vectorHeap[1];
		final float ambientColorBlue = vectorHeap[2];

		switch (this.light.getType()) {
		case AMBIENT:
		case OMNIDIRECTIONAL:
			cache[0] = this.node.worldLocation.x;
			cache[1] = this.node.worldLocation.y;
			cache[2] = this.node.worldLocation.z;
			break;
		case DIRECTIONAL:
			vector3Heap.set(0, 0, 1);
			this.node.localRotation.transform(vector3Heap);
			vector3Heap.nor();
			cache[0] = vector3Heap.x;
			cache[1] = vector3Heap.y;
			cache[2] = vector3Heap.z;
			break;
		default:
			cache[0] = 0;
			cache[1] = 0;
			cache[2] = 0;
			break;
		}

		cache[3] = this.instance.worldLocation.z;
		cache[4] = this.light.getType().ordinal();
		cache[5] = attenuationStart;
		cache[6] = attenuationEnd;
		cache[7] = 0;
		cache[8] = colorRed;
		cache[9] = colorGreen;
		cache[10] = colorBlue;
		cache[11] = intensity;
		cache[12] = ambientColorRed;
		cache[13] = ambientColorGreen;
		cache[14] = ambientColorBlue;
		cache[15] = ambientIntensity;
	}

	@Override
	public void update(final float dt, final boolean visible) {
	}

	public void update(final Scene scene) {
		this.light.getVisibility(scalarHeap, this.instance.sequence, this.instance.frame, this.instance.counter);
		this.visible = scalarHeap[0] > 0;
		updateVisibility(scene, this.visible);
	}

	public void remove(final Scene scene) {
		updateVisibility(scene, false);
	}

	private void updateVisibility(final Scene scene, final boolean visible) {
		if (scene != null) {
			if (this.loadedInScene != visible) {
				if (visible) {
					scene.addLight(this);
				}
				else {
					scene.removeLight(this);
				}
				this.loadedInScene = visible;
			}
		}
	}
}
