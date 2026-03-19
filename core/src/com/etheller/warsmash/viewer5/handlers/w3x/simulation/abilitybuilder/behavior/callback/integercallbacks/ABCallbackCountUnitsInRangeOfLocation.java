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
	
	private int count = 0;
	
	// ⚡ Bolt Optimization: Cache enum function to prevent per-tick GC allocations during spatial queries
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
		count = 0; 
		
		CountUnitsInRangeOfLocationEnum enumFunction = this.enumFunctionPool.obtain();
		try {
			game.getWorldCollision().enumUnitsInRect(recycleRect,
					enumFunction.reset(origin, rangeVal));
			count = enumFunction.getCount();
		} finally {
			enumFunction.clear();
			this.enumFunctionPool.free(enumFunction);
		}
		return count;
	}

	private static final class CountUnitsInRangeOfLocationEnum implements CUnitEnumFunction {
		private AbilityPointTarget origin;
		private float rangeVal;
		private int count = 0;

		public CountUnitsInRangeOfLocationEnum reset(final AbilityPointTarget origin, final float rangeVal) {
			this.origin = origin;
			this.rangeVal = rangeVal;
			this.count = 0;
			return this;
		}

		public void clear() {
			this.origin = null;
		}

		public int getCount() {
			return this.count;
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
