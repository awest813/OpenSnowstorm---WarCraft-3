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
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.iterstructs.UnitAndRange;

public class ABCallbackGetNearestCorpseInRangeOfUnit extends ABUnitCallback {
	private static final Rectangle recycleRect = new Rectangle();

	private ABUnitCallback originUnit;
	private ABFloatCallback range;
	private List<ABCondition> conditions;
	
	private static final Pool<CorpseEnumFunction> pool = new Pool<CorpseEnumFunction>() {
		@Override
		protected CorpseEnumFunction newObject() {
			return new CorpseEnumFunction();
		}
	};

	private static final class CorpseEnumFunction implements CUnitEnumFunction {
		private CUnit originUnitTarget;
		private float rangeVal;
		private UnitAndRange ur;
		private List<ABCondition> conditions;
		private Map<String, Object> localStore;
		private int castId;
		private CSimulation game;
		private CUnit caster;

		public CorpseEnumFunction reset(CUnit originUnitTarget, float rangeVal, UnitAndRange ur,
				List<ABCondition> conditions, Map<String, Object> localStore, int castId, CSimulation game, CUnit caster) {
			this.originUnitTarget = originUnitTarget;
			this.rangeVal = rangeVal;
			this.ur = ur;
			this.conditions = conditions;
			this.localStore = localStore;
			this.castId = castId;
			this.game = game;
			this.caster = caster;
			return this;
		}

		@Override
		public boolean call(final CUnit enumUnit) {
			if (originUnitTarget.canReach(enumUnit, rangeVal)) {
				double dist = originUnitTarget.distance(enumUnit);
				if (ur.getUnit() == null || ur.getRange() > dist) {
					if (conditions != null) {
						boolean result = true;
						localStore.put(ABLocalStoreKeys.MATCHINGUNIT + castId, enumUnit);
						for (ABCondition condition : conditions) {
							result = result && condition.evaluate(game, caster, localStore, castId);
						}
						localStore.remove(ABLocalStoreKeys.MATCHINGUNIT + castId);
						if (result) {
							ur.setRange(dist);
							ur.setUnit(enumUnit);
						}
					} else {
						ur.setRange(dist);
						ur.setUnit(enumUnit);
					}
				}
			}
			return false;
		}
	}

	@Override
	public CUnit callback(CSimulation game, CUnit caster, Map<String, Object> localStore, final int castId) {
		CUnit originUnitTarget = originUnit.callback(game, caster, localStore, castId);
		Float rangeValObj = range.callback(game, caster, localStore, castId);
		float rangeVal = rangeValObj != null ? rangeValObj : 0f;
		
		final UnitAndRange ur = new UnitAndRange();
		
		recycleRect.set(originUnitTarget.getX() - rangeVal, originUnitTarget.getY() - rangeVal, rangeVal * 2,
				rangeVal * 2);

		CorpseEnumFunction enumFunction = pool.obtain().reset(originUnitTarget, rangeVal, ur, conditions, localStore, castId, game, caster);
		try {
			game.getWorldCollision().enumCorpsesInRect(recycleRect, enumFunction);
		} finally {
			enumFunction.reset(null, 0, null, null, null, 0, null, null);
			pool.free(enumFunction);
		}

		return ur.getUnit();
	}

}
