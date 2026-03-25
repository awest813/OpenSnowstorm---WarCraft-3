package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitEnumFunction;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.GetAbilityByRawcodeVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic.CLevelingAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPassiveSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponent;

public abstract class CAbilityAuraBase extends CAbilityPassiveSpellBase {
	private static final float AURA_PERIODIC_CHECK_TIME = 2.00f;
	private static final int AURA_PERIODIC_CHECK_TIME_TICKS = (int) (Math
			.ceil(AURA_PERIODIC_CHECK_TIME / WarsmashConstants.SIMULATION_STEP_TIME));
	private War3ID buffId;
	private SimulationRenderComponent fx;
	private int nextAreaCheck = 0;

	// ⚡ Bolt: Cache the callback instance to prevent GC allocations
	private final EnumUnitsInRange enumUnitsInRange = new EnumUnitsInRange();

	public CAbilityAuraBase(final int handleId, final War3ID code, final War3ID alias) {
		super(handleId, code, alias);
	}

	@Override
	public final void populateData(final GameObject worldEditorAbility, final int level) {
		this.buffId = AbstractCAbilityTypeDefinition.getBuffId(worldEditorAbility, level);
		populateAuraData(worldEditorAbility, level);
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit unit) {
		this.fx = game.createPersistentSpellEffectOnUnit(unit, getAlias(), CEffectType.TARGET, 0);
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit unit) {
		this.fx.remove();
	}

	@Override
	public void onTick(final CSimulation game, final CUnit source) {
		final int gameTurnTick = game.getGameTurnTick();
		if (gameTurnTick >= nextAreaCheck) {
			game.getWorldCollision().enumUnitsInRange(source.getX(), source.getY(), getAreaOfEffect(),
					this.enumUnitsInRange.reset(game, source));
			nextAreaCheck = gameTurnTick + AURA_PERIODIC_CHECK_TIME_TICKS;
		}
	}

	private final class EnumUnitsInRange implements CUnitEnumFunction {
		private CSimulation game;
		private CUnit source;

		public EnumUnitsInRange reset(final CSimulation game, final CUnit source) {
			this.game = game;
			this.source = source;
			return this;
		}

		@Override
		public boolean call(final CUnit enumUnit) {
			if (enumUnit.canBeTargetedBy(this.game, this.source, getTargetsAllowed())) {
				// TODO: the below system of adding an ability instead leveling it should maybe
				// be standardized
				final CLevelingAbility existingBuff = enumUnit
						.getAbility(GetAbilityByRawcodeVisitor.getInstance().reset(getBuffId()));
				boolean addNewBuff = false;
				final int level = getLevel();
				if (existingBuff == null) {
					addNewBuff = true;
				}
				else {
					if (existingBuff.getLevel() < level) {
						enumUnit.remove(this.game, existingBuff);
						addNewBuff = true;
					}
				}
				if (addNewBuff) {
					final CBuffAuraBase buff = createBuff(this.game.getHandleIdAllocator().createId(), this.source, enumUnit);
					buff.setAuraSourceUnit(this.source);
					buff.setAuraSourceAbility(CAbilityAuraBase.this);
					buff.setLevel(this.game, this.source, level);
					enumUnit.add(this.game, buff);
				}
			}
			return false;
		}
	}

	protected abstract CBuffAuraBase createBuff(int handleId, CUnit source, CUnit enumUnit);

	public abstract void populateAuraData(GameObject worldEditorAbility, int level);

	public War3ID getBuffId() {
		return buffId;
	}

	public void setBuffId(final War3ID buffId) {
		this.buffId = buffId;
	}
}
