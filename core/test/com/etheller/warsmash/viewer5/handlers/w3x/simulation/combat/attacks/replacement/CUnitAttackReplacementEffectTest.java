package com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.replacement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CWeaponType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackEffectListenerStacking;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackPreDamageListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackPreDamageListenerDamageModResult;

class CUnitAttackReplacementEffectTest {

	@Test
	void defaultConstructorUsesSafeDefaults() {
		final CUnitAttackReplacementEffect effect = new CUnitAttackReplacementEffect();

		assertTrue(effect.getPreDamageListeners().isEmpty());
		assertEquals(0f, effect.getProjectileArc());
		assertEquals(0, effect.getProjectileSpeed());
		assertFalse(effect.isProjectileHomingEnabled());
		assertNull(effect.getProjectileArt());
	}

	@Test
	void constructorCopiesListenerListDefensively() {
		final List<CUnitAttackPreDamageListener> listeners = new ArrayList<>();
		listeners.add(new NoOpPreDamageListener());
		final CUnitAttackReplacementEffect effect = new CUnitAttackReplacementEffect(listeners, 0.25f, true, 900,
				"Abilities\\Weapons\\RocketMissile\\RocketMissile.mdl");

		listeners.clear();
		assertEquals(1, effect.getPreDamageListeners().size());
		assertThrows(UnsupportedOperationException.class,
				() -> effect.getPreDamageListeners().add(new NoOpPreDamageListener()));
	}

	@Test
	void chargeResourcesValidatesArgumentsAndUsesConfiguredValue() {
		final CUnitAttackReplacementEffect denyAttackEffect = new CUnitAttackReplacementEffect(List.of(), 0f, false, 0,
				null, false);
		assertThrows(NullPointerException.class, () -> denyAttackEffect.chargeResources(null));

	}

	private static final class NoOpPreDamageListener implements CUnitAttackPreDamageListener {
		@Override
		public CUnitAttackEffectListenerStacking onAttack(
				final com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation simulation,
				final CUnit attacker,
				final AbilityTarget target,
				final CWeaponType weaponType,
				final CAttackType attackType,
				final CDamageType damageType,
				final CUnitAttackPreDamageListenerDamageModResult result) {
			return CUnitAttackEffectListenerStacking.ALLOW_STACKING;
		}
	}
}
