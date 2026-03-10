package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.behavior.action.structural;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.math.Rectangle;
import com.etheller.warsmash.parsers.jass.JassTextGenerator;
import com.etheller.warsmash.parsers.jass.JassTextGeneratorCallStmt;
import com.etheller.warsmash.parsers.jass.JassTextGeneratorStmt;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.badlogic.gdx.utils.Pool;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitEnumFunction;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.behavior.callback.floatcallbacks.ABFloatCallback;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.behavior.callback.unitcallbacks.ABUnitCallback;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.core.ABAction;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.core.ABCondition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.core.ABLocalStoreKeys;

public class ABActionIterateUnitsInRangeOfUnitMatchingCondition implements ABAction {

	private static final Rectangle recycleRect = new Rectangle();
	private List<ABAction> iterationActions;
	private ABCondition condition;

	private ABUnitCallback originUnit;
	private ABFloatCallback range;

	// ⚡ Bolt Optimization: Use an object pool for the CUnitEnumFunction to prevent allocating an anonymous class
	// on every tick during spatial queries, while avoiding re-entrancy bugs. This reduces GC pressure safely.
	private final Pool<IterateUnitsInRangeOfUnitMatchingConditionEnum> enumFunctionPool = new Pool<IterateUnitsInRangeOfUnitMatchingConditionEnum>() {
		@Override
		protected IterateUnitsInRangeOfUnitMatchingConditionEnum newObject() {
			return new IterateUnitsInRangeOfUnitMatchingConditionEnum();
		}
	};

	@Override
	public void runAction(final CSimulation game, final CUnit caster, final Map<String, Object> localStore,
			final int castId) {
		final CUnit originUnitTarget = this.originUnit.callback(game, caster, localStore, castId);
		final Float rangeVal = this.range.callback(game, caster, localStore, castId);

		recycleRect.set(originUnitTarget.getX() - rangeVal, originUnitTarget.getY() - rangeVal, rangeVal * 2,
				rangeVal * 2);
		IterateUnitsInRangeOfUnitMatchingConditionEnum enumFunction = this.enumFunctionPool.obtain();
		try {
			game.getWorldCollision().enumUnitsInRect(recycleRect,
					enumFunction.reset(game, caster, localStore, castId, originUnitTarget, rangeVal));
		} finally {
			enumFunction.clear(); // Clear references to prevent memory leaks even if exception occurs
			this.enumFunctionPool.free(enumFunction);
		}
		localStore.remove(ABLocalStoreKeys.ENUMUNIT + castId);
		localStore.remove(ABLocalStoreKeys.MATCHINGUNIT + castId);
	}

	private final class IterateUnitsInRangeOfUnitMatchingConditionEnum implements CUnitEnumFunction {
		private CSimulation game;
		private CUnit caster;
		private Map<String, Object> localStore;
		private int castId;
		private CUnit originUnitTarget;
		private float rangeVal;

		public IterateUnitsInRangeOfUnitMatchingConditionEnum reset(final CSimulation game, final CUnit caster, final Map<String, Object> localStore,
				final int castId, final CUnit originUnitTarget, final float rangeVal) {
			this.game = game;
			this.caster = caster;
			this.localStore = localStore;
			this.castId = castId;
			this.originUnitTarget = originUnitTarget;
			this.rangeVal = rangeVal;
			return this;
		}

		public void clear() {
			this.game = null;
			this.caster = null;
			this.localStore = null;
			this.originUnitTarget = null;
		}

		@Override
		public boolean call(final CUnit enumUnit) {
			if (this.originUnitTarget.canReach(enumUnit, this.rangeVal)) {
				this.localStore.put(ABLocalStoreKeys.MATCHINGUNIT + this.castId, enumUnit);
				if ((ABActionIterateUnitsInRangeOfUnitMatchingCondition.this.condition == null)
						|| ABActionIterateUnitsInRangeOfUnitMatchingCondition.this.condition.evaluate(this.game, this.caster,
								this.localStore, this.castId)) {
					this.localStore.put(ABLocalStoreKeys.ENUMUNIT + this.castId, enumUnit);
					for (final ABAction iterationAction : ABActionIterateUnitsInRangeOfUnitMatchingCondition.this.iterationActions) {
						iterationAction.runAction(this.game, this.caster, this.localStore, this.castId);
					}
				}
			}
			return false;
		}
	}

	@Override
	public void generateJassEquivalent(int indent, JassTextGenerator jassTextGenerator) {
		final List<JassTextGeneratorStmt> modifiedActionList = new ArrayList<>(this.iterationActions);
		modifiedActionList.add(0, new JassTextGeneratorCallStmt() {
			@Override
			public String generateJassEquivalent(final JassTextGenerator jassTextGenerator) {
				return "SetLocalStoreUnitHandle(" + jassTextGenerator.getTriggerLocalStore()
						+ ", AB_LOCAL_STORE_KEY_ENUMUNIT + I2S(" + jassTextGenerator.getCastId() + "), "
						+ "GetFilterUnit()" + ") // filter unit used intentionally as enum";
			}
		});
		if (this.condition != null) {
			modifiedActionList.add(0, new JassTextGeneratorStmt() {
				@Override
				public void generateJassEquivalent(int indent, final JassTextGenerator jassTextGenerator) {
					final StringBuilder sb = new StringBuilder();
					JassTextGenerator.Util.indent(indent, sb);
					sb.append("SetLocalStoreUnitHandle(" + jassTextGenerator.getTriggerLocalStore()
							+ ", AB_LOCAL_STORE_KEY_MATCHINGUNIT + I2S(" + jassTextGenerator.getCastId() + "), "
							+ "GetFilterUnit()" + ")");
					jassTextGenerator.println(sb.toString());

					sb.setLength(0);
					JassTextGenerator.Util.indent(indent, sb);
					sb.append("if not ");
					sb.append(ABActionIterateUnitsInRangeOfUnitMatchingCondition.this.condition
							.generateJassEquivalent(jassTextGenerator));
					sb.append(" then");
					jassTextGenerator.println(sb.toString());

					sb.setLength(0);
					JassTextGenerator.Util.indent(indent + 1, sb);
					sb.append("return false");
					jassTextGenerator.println(sb.toString());

					sb.setLength(0);
					JassTextGenerator.Util.indent(indent, sb);
					sb.append("endif");
					jassTextGenerator.println(sb.toString());
				}
			});
		}
		modifiedActionList.add(new JassTextGeneratorStmt() {
			@Override
			public void generateJassEquivalent(int indent, JassTextGenerator jassTextGenerator) {
				final StringBuilder sb = new StringBuilder();
				JassTextGenerator.Util.indent(indent, sb);
				sb.append("return false");
				jassTextGenerator.println(sb.toString());
			}
		});
		final String iterationActionsName = jassTextGenerator.createAnonymousFunction(modifiedActionList,
				"UnitsInRangeOfUnitEnumActions");

		final StringBuilder sb = new StringBuilder();
		JassTextGenerator.Util.indent(indent, sb);
		sb.append("// TODO: use a global filter");
		jassTextGenerator.println(sb.toString());

		sb.setLength(0);
		JassTextGenerator.Util.indent(indent, sb);
		sb.append("call GroupEnumUnitsInRangeOfUnit(au_tempGroup, "
				+ this.originUnit.generateJassEquivalent(jassTextGenerator) + ", "
				+ this.range.generateJassEquivalent(jassTextGenerator) + ", Filter("
				+ jassTextGenerator.functionPointerByName(iterationActionsName) + "))");
		jassTextGenerator.println(sb.toString());

		sb.setLength(0);
		JassTextGenerator.Util.indent(indent, sb);
		sb.append("call FlushChildLocalStore(" + jassTextGenerator.getTriggerLocalStore()
				+ ", AB_LOCAL_STORE_KEY_ENUMUNIT + I2S(" + jassTextGenerator.getCastId() + "))");
		jassTextGenerator.println(sb.toString());

		sb.setLength(0);
		JassTextGenerator.Util.indent(indent, sb);
		sb.append("call FlushChildLocalStore(" + jassTextGenerator.getTriggerLocalStore()
				+ ", AB_LOCAL_STORE_KEY_MATCHINGUNIT + I2S(" + jassTextGenerator.getCastId() + "))");
		jassTextGenerator.println(sb.toString());
	}
}
