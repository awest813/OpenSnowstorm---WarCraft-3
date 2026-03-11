package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.behavior.callback.integercallbacks;

import java.util.Map;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pool.Poolable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitEnumFunction;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.behavior.callback.floatcallbacks.ABFloatCallback;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.behavior.callback.locationcallbacks.ABLocationCallback;

public class ABCallbackCountUnitsInRangeOfLocation extends ABIntegerCallback {

	private static final Rectangle recycleRect = new Rectangle();
	private static final Pool<CountUnitsEnum> pool = new Pool<CountUnitsEnum>() {
		@Override
		protected CountUnitsEnum newObject() {
			return new CountUnitsEnum();
		}
	};
	private ABLocationCallback location;
	private ABFloatCallback range;
	
	@Override
	public Integer callback(CSimulation game, CUnit caster, Map<String, Object> localStore, final int castId) {
		AbilityPointTarget origin = location.callback(game, caster, localStore, castId);
		Float rangeVal = range.callback(game, caster, localStore, castId);
		
		recycleRect.set(origin.getX() - rangeVal, origin.getY() - rangeVal, rangeVal * 2,
				rangeVal * 2);

		CountUnitsEnum enumFunction = pool.obtain();
		try {
			enumFunction.reset(origin, rangeVal);
			game.getWorldCollision().enumUnitsInRect(recycleRect, enumFunction);
			return enumFunction.count;
		} finally {
			pool.free(enumFunction);
		}
	}

	private static class CountUnitsEnum implements CUnitEnumFunction, Poolable {
		private AbilityPointTarget origin;
		private float rangeVal;
		private int count = 0;

		public CountUnitsEnum reset(AbilityPointTarget origin, float rangeVal) {
			this.origin = origin;
			this.rangeVal = rangeVal;
			this.count = 0;
			return this;
		}

		@Override
		public boolean call(CUnit enumUnit) {
			if (enumUnit.canReach(origin, rangeVal)) {
				count++;
			}
			return false;
		}

		@Override
		public void reset() {
			origin = null;
		}
	}
}
