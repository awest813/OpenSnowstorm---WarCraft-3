package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.bloodmage.phoenix;

import java.util.EnumSet;

import com.badlogic.gdx.math.Rectangle;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic.AbstractGenericNoIconAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehavior;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttackListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

public class CAbilityPhoenixFire extends AbstractGenericNoIconAbility {

	private float initialDamage;
	private float damagePerSecond;
	private float areaOfEffect;
	private float cooldown;
	private float duration;
	private EnumSet<CTargetType> targetsAllowed;

	private int lastAttackTurnTick;
	private final EnumUnitsInRect enumUnitsInRect = new EnumUnitsInRect();

	public CAbilityPhoenixFire(final int handleId, final War3ID code, final War3ID alias, final float initialDamage,
			final float damagePerSecond, final float areaOfEffect, final float cooldown, final float duration,
			final EnumSet<CTargetType> targetsAllowed) {
		super(handleId, code, alias);
		this.initialDamage = initialDamage;
		this.damagePerSecond = damagePerSecond;
		this.areaOfEffect = areaOfEffect;
		this.cooldown = cooldown;
		this.duration = duration;
		this.targetsAllowed = targetsAllowed;
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit unit) {
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit unit) {
	}

	private final Rectangle recycleRect = new Rectangle();

	@Override
	public void onTick(final CSimulation game, final CUnit unit) {
		final int cooldownTicks = (int) (this.cooldown / WarsmashConstants.SIMULATION_STEP_TIME);
		final int gameTurnTick = game.getGameTurnTick();
		if (gameTurnTick > (this.lastAttackTurnTick + cooldownTicks)) {
			final EnumUnitsInRect enumFunction = this.enumUnitsInRect.reset(game, unit, gameTurnTick);
			game.getWorldCollision().enumUnitsInRect(this.recycleRect.set(unit.getX() - this.areaOfEffect,
					unit.getY() - this.areaOfEffect, this.areaOfEffect * 2, this.areaOfEffect * 2), enumFunction);
			enumFunction.clear();
		}
	}

	@Override
	public void onDeath(final CSimulation game, final CUnit cUnit) {

	}

	@Override
	public void onCancelFromQueue(final CSimulation game, final CUnit unit, final int orderId) {

	}

	@Override
	public CBehavior begin(final CSimulation game, final CUnit caster, final int orderId, final CWidget target) {
		return null;
	}

	@Override
	public CBehavior begin(final CSimulation game, final CUnit caster, final int orderId,
			final AbilityPointTarget point) {
		return null;
	}

	@Override
	public CBehavior beginNoTarget(final CSimulation game, final CUnit caster, final int orderId) {
		return null;
	}

	@Override
	public void checkCanTarget(final CSimulation game, final CUnit unit, final int orderId, final CWidget target,
			final AbilityTargetCheckReceiver<CWidget> receiver) {
		receiver.notAnActiveAbility();
	}

	@Override
	public void checkCanTarget(final CSimulation game, final CUnit unit, final int orderId,
			final AbilityPointTarget target, final AbilityTargetCheckReceiver<AbilityPointTarget> receiver) {
		receiver.notAnActiveAbility();
	}

	@Override
	public void checkCanTargetNoTarget(final CSimulation game, final CUnit unit, final int orderId,
			final AbilityTargetCheckReceiver<Void> receiver) {
		receiver.notAnActiveAbility();
	}

	@Override
	protected void innerCheckCanUse(final CSimulation game, final CUnit unit, final int orderId,
			final AbilityActivationReceiver receiver) {
		receiver.notAnActiveAbility();
	}

	public float getInitialDamage() {
		return this.initialDamage;
	}

	public float getDamagePerSecond() {
		return this.damagePerSecond;
	}

	public float getAreaOfEffect() {
		return this.areaOfEffect;
	}

	public float getCooldown() {
		return this.cooldown;
	}

	public float getDuration() {
		return this.duration;
	}

	public EnumSet<CTargetType> getTargetsAllowed() {
		return this.targetsAllowed;
	}

	public void setInitialDamage(final float initialDamage) {
		this.initialDamage = initialDamage;
	}

	public void setDamagePerSecond(final float damagePerSecond) {
		this.damagePerSecond = damagePerSecond;
	}

	public void setAreaOfEffect(final float areaOfEffect) {
		this.areaOfEffect = areaOfEffect;
	}

	public void setCooldown(final float cooldown) {
		this.cooldown = cooldown;
	}

	public void setDuration(final float duration) {
		this.duration = duration;
	}

	public void setTargetsAllowed(final EnumSet<CTargetType> targetsAllowed) {
		this.targetsAllowed = targetsAllowed;
	}

	/**
	 * ⚡ Bolt: Implemented cached inner class instead of allocating a CUnitEnumFunction
	 * lambda per tick to save memory and avoid triggering GC pauses during spatial
	 * query operations. Measurement: Reduces allocation footprint by 1 closure object
	 * per cooldown tick. Clears internal references afterward to prevent memory leaks.
	 */
	private final class EnumUnitsInRect implements com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitEnumFunction {
		private CSimulation game;
		private CUnit unit;
		private int gameTurnTick;

		public EnumUnitsInRect reset(CSimulation game, CUnit unit, int gameTurnTick) {
			this.game = game;
			this.unit = unit;
			this.gameTurnTick = gameTurnTick;
			return this;
		}

		public void clear() {
			this.game = null;
			this.unit = null;
		}

		@Override
		public boolean call(final CUnit enumUnit) {
			if (unit.canReach(enumUnit, CAbilityPhoenixFire.this.areaOfEffect)
					&& enumUnit.canBeTargetedBy(game, unit, CAbilityPhoenixFire.this.targetsAllowed)) {
				unit.getCurrentAttacks().get(0).launch(game, unit, enumUnit, CAbilityPhoenixFire.this.initialDamage,
						CUnitAttackListener.DO_NOTHING);
				CAbilityPhoenixFire.this.lastAttackTurnTick = gameTurnTick;
//				return true;
			}
			return false;
		}
	}

}
