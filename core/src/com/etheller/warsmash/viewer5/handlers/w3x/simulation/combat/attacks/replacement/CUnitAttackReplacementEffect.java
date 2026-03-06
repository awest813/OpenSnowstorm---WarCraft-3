package com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.replacement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackPreDamageListener;

public class CUnitAttackReplacementEffect {
	private final List<CUnitAttackPreDamageListener> preDamageListeners;
	private final float projectileArc;
	private final boolean projectileHomingEnabled;
	private final int projectileSpeed;
	private final String projectileArt;
	private final boolean allowAttack;

	public CUnitAttackReplacementEffect() {
		this(Collections.emptyList(), 0, false, 0, null, true);
	}

	public CUnitAttackReplacementEffect(final List<CUnitAttackPreDamageListener> preDamageListeners,
			final float projectileArc, final boolean projectileHomingEnabled, final int projectileSpeed,
			final String projectileArt) {
		this(preDamageListeners, projectileArc, projectileHomingEnabled, projectileSpeed, projectileArt, true);
	}

	public CUnitAttackReplacementEffect(final List<CUnitAttackPreDamageListener> preDamageListeners,
			final float projectileArc, final boolean projectileHomingEnabled, final int projectileSpeed,
			final String projectileArt, final boolean allowAttack) {
		if (preDamageListeners == null || preDamageListeners.isEmpty()) {
			this.preDamageListeners = Collections.emptyList();
		}
		else {
			this.preDamageListeners = Collections.unmodifiableList(new ArrayList<>(preDamageListeners));
		}
		this.projectileArc = projectileArc;
		this.projectileHomingEnabled = projectileHomingEnabled;
		this.projectileSpeed = projectileSpeed;
		this.projectileArt = projectileArt;
		this.allowAttack = allowAttack;
	}

	public List<CUnitAttackPreDamageListener> getPreDamageListeners() {
		return this.preDamageListeners;
	}

	public float getProjectileArc() {
		return this.projectileArc;
	}

	public boolean isProjectileHomingEnabled() {
		return this.projectileHomingEnabled;
	}

	public int getProjectileSpeed() {
		return this.projectileSpeed;
	}

	public String getProjectileArt() {
		return this.projectileArt;
	}

	public boolean chargeResources(CUnit unit) {
		Objects.requireNonNull(unit, "unit");
		return this.allowAttack;
	}

}
