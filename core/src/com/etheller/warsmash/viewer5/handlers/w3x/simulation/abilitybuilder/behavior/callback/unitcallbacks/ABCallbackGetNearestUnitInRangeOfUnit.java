package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.behavior.callback.unitcallbacks;

import java.util.List;
import java.util.Map;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Pool;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitEnumFunction;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.behavior.callback.floatcallbacks.ABFloatCallback;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.core.ABCondition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.core.ABLocalStoreKeys;

public class ABCallbackGetNearestUnitInRangeOfUnit extends ABUnitCallback {
	private static final Rectangle recycleRect = new Rectangle();

	private ABUnitCallback originUnit;
	private ABFloatCallback range;
	private List<ABCondition> conditions;
	
	// ⚡ Bolt Optimization: Use an object pool for the CUnitEnumFunction to prevent allocating an anonymous class
	// on every tick during spatial queries, while avoiding re-entrancy bugs. This reduces GC pressure safely.
	private final Pool<GetNearestUnitEnum> enumFunctionPool = new Pool<GetNearestUnitEnum>() {
		@Override
		protected GetNearestUnitEnum newObject() {
			return new GetNearestUnitEnum();
		}
	};

	@Override
	public CUnit callback(CSimulation game, CUnit caster, Map<String, Object> localStore, final int castId) {
		CUnit originUnitTarget = originUnit.callback(game, caster, localStore, castId);
		Float rangeVal = range.callback(game, caster, localStore, castId);
		
		recycleRect.set(originUnitTarget.getX() - rangeVal, originUnitTarget.getY() - rangeVal, rangeVal * 2,
				rangeVal * 2);

		GetNearestUnitEnum enumFunction = this.enumFunctionPool.obtain();
		try {
			game.getWorldCollision().enumUnitsInRect(recycleRect,
					enumFunction.reset(game, caster, localStore, castId, originUnitTarget, rangeVal, this.conditions));
			return enumFunction.foundUnit;
		} finally {
			enumFunction.clear(); // Clear references to prevent memory leaks even if exception occurs
			this.enumFunctionPool.free(enumFunction);
		}
	}

	private static final class GetNearestUnitEnum implements CUnitEnumFunction {
		private CSimulation game;
		private CUnit caster;
		private Map<String, Object> localStore;
		private int castId;
		private CUnit originUnitTarget;
		private float rangeVal;
		private List<ABCondition> conditions;

		private CUnit foundUnit;
		private double foundRange;

		public GetNearestUnitEnum reset(final CSimulation game, final CUnit caster, final Map<String, Object> localStore,
				final int castId, final CUnit originUnitTarget, final float rangeVal, final List<ABCondition> conditions) {
			this.game = game;
			this.caster = caster;
			this.localStore = localStore;
			this.castId = castId;
			this.originUnitTarget = originUnitTarget;
			this.rangeVal = rangeVal;
			this.conditions = conditions;
			this.foundUnit = null;
			this.foundRange = 0;
			return this;
		}

		public void clear() {
			this.game = null;
			this.caster = null;
			this.localStore = null;
			this.originUnitTarget = null;
			this.conditions = null;
			this.foundUnit = null;
		}

		@Override
		public boolean call(final CUnit enumUnit) {
			if (this.originUnitTarget.canReach(enumUnit, this.rangeVal)) {
				double dist = this.originUnitTarget.distance(enumUnit);
				if (this.foundUnit == null || this.foundRange > dist) {
					if (this.conditions != null) {
						boolean result = true;
						this.localStore.put(ABLocalStoreKeys.MATCHINGUNIT + this.castId, enumUnit);
						for (ABCondition condition : this.conditions) {
							result = result && condition.evaluate(this.game, this.caster, this.localStore, this.castId);
						}
						this.localStore.remove(ABLocalStoreKeys.MATCHINGUNIT + this.castId);
						if (result) {
							this.foundRange = dist;
							this.foundUnit = enumUnit;
						}
					} else {
						this.foundRange = dist;
						this.foundUnit = enumUnit;
					}
				}
			}
			return false;
		}
	}

}
