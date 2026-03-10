package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.behavior.action.structural;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.math.Rectangle;
import com.etheller.warsmash.parsers.jass.JassTextGenerator;
import com.etheller.warsmash.parsers.jass.JassTextGeneratorCallStmt;
import com.etheller.warsmash.parsers.jass.JassTextGeneratorStmt;
import com.badlogic.gdx.utils.Pool;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitEnumFunction;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.core.ABAction;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.core.ABLocalStoreKeys;

public class ABActionIterateUnitsInRect implements ABAction {

	private static final Rectangle recycleRect = new Rectangle();
	private Integer x1;
	private Integer x2;
	private Integer y1;
	private Integer y2;
	private List<ABAction> iterationActions;

	// ⚡ Bolt Optimization: Use an object pool for the CUnitEnumFunction to prevent allocating an anonymous class
	// on every tick during spatial queries, while avoiding re-entrancy bugs. This reduces GC pressure safely.
	private final Pool<IterateUnitsInRectEnum> enumFunctionPool = new Pool<IterateUnitsInRectEnum>() {
		@Override
		protected IterateUnitsInRectEnum newObject() {
			return new IterateUnitsInRectEnum();
		}
	};

	@Override
	public void runAction(final CSimulation game, final CUnit caster, final Map<String, Object> localStore,
			final int castId) {
		recycleRect.set(this.x1, this.y1, this.x2 - this.x1, this.y2 - this.y1);
		IterateUnitsInRectEnum enumFunction = this.enumFunctionPool.obtain();
		try {
			game.getWorldCollision().enumUnitsInRect(recycleRect,
					enumFunction.reset(game, caster, localStore, castId));
		} finally {
			enumFunction.clear(); // Clear references to prevent memory leaks even if exception occurs
			this.enumFunctionPool.free(enumFunction);
		}
		localStore.remove(ABLocalStoreKeys.ENUMUNIT + castId);
	}

	private final class IterateUnitsInRectEnum implements CUnitEnumFunction {
		private CSimulation game;
		private CUnit caster;
		private Map<String, Object> localStore;
		private int castId;

		public IterateUnitsInRectEnum reset(final CSimulation game, final CUnit caster, final Map<String, Object> localStore,
				final int castId) {
			this.game = game;
			this.caster = caster;
			this.localStore = localStore;
			this.castId = castId;
			return this;
		}

		public void clear() {
			this.game = null;
			this.caster = null;
			this.localStore = null;
		}

		@Override
		public boolean call(final CUnit enumUnit) {
			this.localStore.put(ABLocalStoreKeys.ENUMUNIT + this.castId, enumUnit);
			for (final ABAction iterationAction : ABActionIterateUnitsInRect.this.iterationActions) {
				iterationAction.runAction(this.game, this.caster, this.localStore, this.castId);
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
				"UnitsInRectEnumActions");

		final StringBuilder sb = new StringBuilder();
		JassTextGenerator.Util.indent(indent, sb);
		sb.append("// TODO: use a global filter");
		jassTextGenerator.println(sb.toString());

		sb.setLength(0);
		JassTextGenerator.Util.indent(indent, sb);
		sb.append("call GroupEnumUnitsInRect(au_tempGroup, Rect(" + this.x1 + ", " + this.y1 + ", " + this.x2 + ", "
				+ this.y2 + "), Filter(" + jassTextGenerator.functionPointerByName(iterationActionsName) + "))");
		jassTextGenerator.println(sb.toString());

		sb.setLength(0);
		JassTextGenerator.Util.indent(indent, sb);
		sb.append("call FlushChildLocalStore(" + jassTextGenerator.getTriggerLocalStore()
				+ ", AB_LOCAL_STORE_KEY_ENUMUNIT + I2S(" + jassTextGenerator.getCastId() + "))");
		jassTextGenerator.println(sb.toString());
	}
}
