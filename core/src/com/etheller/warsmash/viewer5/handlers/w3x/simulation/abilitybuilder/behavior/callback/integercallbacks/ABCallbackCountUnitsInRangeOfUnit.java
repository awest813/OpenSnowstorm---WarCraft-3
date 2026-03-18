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
	private ABUnitCallback unit;
	private ABFloatCallback range;
	
	private int count = 0;
	
	// ⚡ Bolt Optimization: Cache enum function to prevent per-tick GC allocations during spatial queries
	private final Pool<CountUnitsInRangeOfUnitEnum> enumFunctionPool = new Pool<CountUnitsInRangeOfUnitEnum>() {
		@Override
		protected CountUnitsInRangeOfUnitEnum newObject() {
			return new CountUnitsInRangeOfUnitEnum();
		}
	};

	@Override
	public Integer callback(CSimulation game, CUnit caster, Map<String, Object> localStore, final int castId) {
		CUnit originUnitTarget = unit.callback(game, caster, localStore, castId);
		Float rangeVal = range.callback(game, caster, localStore, castId);
		
		recycleRect.set(originUnitTarget.getX() - rangeVal, originUnitTarget.getY() - rangeVal, rangeVal * 2,
				rangeVal * 2);
		count = 0; 
		
		CountUnitsInRangeOfUnitEnum enumFunction = this.enumFunctionPool.obtain();
		try {
			game.getWorldCollision().enumUnitsInRect(recycleRect,
					enumFunction.reset(originUnitTarget, rangeVal));
			count = enumFunction.getCount();
		} finally {
			enumFunction.clear();
			this.enumFunctionPool.free(enumFunction);
		}
		return count;
	}

	private static final class CountUnitsInRangeOfUnitEnum implements CUnitEnumFunction {
		private CUnit originUnitTarget;
		private float rangeVal;
		private int count = 0;

		public CountUnitsInRangeOfUnitEnum reset(final CUnit originUnitTarget, final float rangeVal) {
			this.originUnitTarget = originUnitTarget;
			this.rangeVal = rangeVal;
			this.count = 0;
			return this;
		}

		public void clear() {
			this.originUnitTarget = null;
		}

		public int getCount() {
			return this.count;
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
