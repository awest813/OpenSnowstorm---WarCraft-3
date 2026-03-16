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

		CountUnitsEnum countEnum = pool.obtain();
		try {
			countEnum.reset(origin, rangeVal);
			game.getWorldCollision().enumUnitsInRect(recycleRect, countEnum);
			return countEnum.count;
		}
		finally {
			countEnum.clear();
			pool.free(countEnum);
		}
	}

	private static final class CountUnitsEnum implements CUnitEnumFunction {
		public AbilityPointTarget origin;
		public float rangeVal;
		public int count;

		public void reset(AbilityPointTarget origin, float rangeVal) {
			this.origin = origin;
			this.rangeVal = rangeVal;
			this.count = 0;
		}

		public void clear() {
			this.origin = null;
		}

		@Override
		public boolean call(final CUnit enumUnit) {
			if (enumUnit.canReach(origin, rangeVal)) {
				count++;
			}
			return false;
		}
	}
}
