package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.behavior.callback.integercallbacks;

import java.util.Map;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Pool;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitEnumFunction;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.behavior.callback.floatcallbacks.ABFloatCallback;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.behavior.callback.locationcallbacks.ABLocationCallback;

public class ABCallbackCountUnitsInRangeOfLocation extends ABIntegerCallback {

	private static final Rectangle recycleRect = new Rectangle();
	private ABLocationCallback location;
	private ABFloatCallback range;
	
	// ⚡ Bolt Optimization: Use an object pool for the CUnitEnumFunction to prevent allocating an anonymous class
	// on every tick during spatial queries, while avoiding re-entrancy bugs. This reduces GC pressure safely.
	private final Pool<CountUnitsInRangeOfLocationEnum> enumFunctionPool = new Pool<CountUnitsInRangeOfLocationEnum>() {
		@Override
		protected CountUnitsInRangeOfLocationEnum newObject() {
			return new CountUnitsInRangeOfLocationEnum();
		}
	};
	
	@Override
	public Integer callback(CSimulation game, CUnit caster, Map<String, Object> localStore, final int castId) {
		AbilityPointTarget origin = location.callback(game, caster, localStore, castId);
		Float rangeVal = range.callback(game, caster, localStore, castId);
		
		recycleRect.set(origin.getX() - rangeVal, origin.getY() - rangeVal, rangeVal * 2,
				rangeVal * 2);
		
		CountUnitsInRangeOfLocationEnum enumFunction = this.enumFunctionPool.obtain();
		try {
			game.getWorldCollision().enumUnitsInRect(recycleRect, enumFunction.reset(origin, rangeVal));
			return enumFunction.count;
		} finally {
			enumFunction.clear(); // Clear references to prevent memory leaks even if exception occurs
			this.enumFunctionPool.free(enumFunction);
		}
	}

	private static final class CountUnitsInRangeOfLocationEnum implements CUnitEnumFunction {
		private AbilityPointTarget origin;
		private float rangeVal;
		private int count;

		public CountUnitsInRangeOfLocationEnum reset(final AbilityPointTarget origin, final float rangeVal) {
			this.origin = origin;
			this.rangeVal = rangeVal;
			this.count = 0;
			return this;
		}

		public void clear() {
			this.origin = null;
		}

		@Override
		public boolean call(final CUnit enumUnit) {
			if (enumUnit.canReach(this.origin, this.rangeVal)) {
				this.count++;
			}
			return false;
		}
	}

}
