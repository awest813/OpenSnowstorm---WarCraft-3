package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.behavior.callback.integercallbacks;

import java.util.Map;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Pool;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitEnumFunction;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.behavior.callback.floatcallbacks.ABFloatCallback;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.behavior.callback.unitcallbacks.ABUnitCallback;

public class ABCallbackCountUnitsInRangeOfUnit extends ABIntegerCallback {

	private static final Rectangle recycleRect = new Rectangle();
	private static final Pool<CountUnitsEnumFunction> enumPool = new Pool<CountUnitsEnumFunction>() {
		@Override
		protected CountUnitsEnumFunction newObject() {
			return new CountUnitsEnumFunction();
		}
	};

	private ABUnitCallback unit;
	private ABFloatCallback range;
	
	@Override
	public Integer callback(CSimulation game, CUnit caster, Map<String, Object> localStore, final int castId) {
		CUnit originUnitTarget = unit.callback(game, caster, localStore, castId);
		Float rangeVal = range.callback(game, caster, localStore, castId);
		
		recycleRect.set(originUnitTarget.getX() - rangeVal, originUnitTarget.getY() - rangeVal, rangeVal * 2,
				rangeVal * 2);
		
		CountUnitsEnumFunction countEnum = enumPool.obtain();
		countEnum.reset(originUnitTarget, rangeVal);
		try {
			game.getWorldCollision().enumUnitsInRect(recycleRect, countEnum);
			return countEnum.count;
		} finally {
			countEnum.clear();
			enumPool.free(countEnum);
		}
	}

	private static final class CountUnitsEnumFunction implements CUnitEnumFunction {
		private CUnit originUnitTarget;
		private float rangeVal;
		public int count;

		public void reset(CUnit originUnitTarget, float rangeVal) {
			this.originUnitTarget = originUnitTarget;
			this.rangeVal = rangeVal;
			this.count = 0;
		}

		public void clear() {
			this.originUnitTarget = null;
		}

		@Override
		public boolean call(final CUnit enumUnit) {
			if (this.originUnitTarget.canReach(enumUnit, this.rangeVal)) {
				this.count++;
			}
			return false;
		}
	}
}
