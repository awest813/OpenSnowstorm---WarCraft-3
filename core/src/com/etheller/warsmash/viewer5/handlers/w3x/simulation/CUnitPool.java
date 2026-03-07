package com.etheller.warsmash.viewer5.handlers.w3x.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.etheller.warsmash.util.War3ID;

/**
 * Represents a JASS unitpool handle.  Units are added via UnitPoolAddUnitType
 * with associated probability weights.  PlaceRandomUnit/GetRandomUnit picks a
 * random unit type using those weights and spawns it.
 */
public class CUnitPool {
	private final List<War3ID> unitTypes = new ArrayList<>();
	private final List<Float> weights = new ArrayList<>();
	private float totalWeight = 0.0f;

	public void addUnitType(final War3ID unitType, final float weight) {
		if (weight <= 0.0f) {
			return;
		}
		this.unitTypes.add(unitType);
		this.weights.add(weight);
		this.totalWeight += weight;
	}

	/**
	 * Picks a random unit type using the stored weights, or returns null if the
	 * pool is empty.
	 */
	public War3ID chooseRandom(final Random random) {
		if (this.totalWeight <= 0.0f || this.unitTypes.isEmpty()) {
			return null;
		}
		float roll = random.nextFloat() * this.totalWeight;
		for (int i = 0; i < this.unitTypes.size(); i++) {
			roll -= this.weights.get(i);
			if (roll <= 0.0f) {
				return this.unitTypes.get(i);
			}
		}
		// Fallback to last entry due to floating-point rounding
		return this.unitTypes.get(this.unitTypes.size() - 1);
	}
}
