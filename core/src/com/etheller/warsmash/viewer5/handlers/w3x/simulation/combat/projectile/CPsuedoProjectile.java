package com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile;


import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.IntMap;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructableEnumFunction;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitEnumFunction;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;

public class CPsuedoProjectile extends CProjectile {

	private static final Rectangle recycleRect = new Rectangle();

	private final CAbilityCollisionProjectileListener projectileListener;

	private int maxHits = 0;
	private int maxHitsPerTarget = 0;

	private int stepInterval = 1;

	private int nextActionTick = 0;

	private float collisionRadius = 0;

	private float finalCollisionRadius = 0;
	private float startingCollisionRadius = 0;
	private float distanceToTarget = 0;

	private IntMap<Integer> collisions = new IntMap<>();
	private int hits = 0;

	private War3ID alias;
	private CEffectType effectType;
	private int effectArtIndex;

	private boolean provideCounts;
	private int artSkip;
	private int artCount = 1;

	private float dx;
	private float dy;
	private int steps;
	
	private int stepCount = 0;

	private CSimulation updateGame;
	private final AbilityPointTarget updateLoc = new AbilityPointTarget(0, 0);
	private int updateDestCount;
	private int updateUnitCount;

	private final CDestructableEnumFunction countDestructablesFunction = new CDestructableEnumFunction() {
		@Override
		public boolean call(CDestructable enumDestructable) {
			if (hits < maxHits && collisions.get(enumDestructable.getHandleId(), 0) < maxHitsPerTarget
					&& enumDestructable.distance(updateLoc.getX(), updateLoc.getY()) < collisionRadius && canHitTarget(updateGame, enumDestructable)) {
				updateDestCount++;
			}
			return false;
		}
	};

	private final CUnitEnumFunction countUnitsFunction = new CUnitEnumFunction() {
		@Override
		public boolean call(final CUnit enumUnit) {
			if (hits < maxHits && collisions.get(enumUnit.getHandleId(), 0) < maxHitsPerTarget
					&& enumUnit.canReach(updateLoc, collisionRadius) && canHitTarget(updateGame, enumUnit)) {
				updateUnitCount++;
			}
			return false;
		}
	};

	private final CDestructableEnumFunction hitDestructablesFunction = new CDestructableEnumFunction() {
		@Override
		public boolean call(CDestructable enumDestructable) {
			if (hits < maxHits && collisions.get(enumDestructable.getHandleId(), 0) < maxHitsPerTarget
					&& enumDestructable.distance(updateLoc.getX(), updateLoc.getY()) < collisionRadius && canHitTarget(updateGame, enumDestructable)) {
				onHitTarget(updateGame, enumDestructable);
				if (maxHits > 0) {
					hits++;
				}
				collisions.put(enumDestructable.getHandleId(), collisions.get(enumDestructable.getHandleId(), 0) + 1);
			}
			return false;
		}
	};

	private final CUnitEnumFunction hitUnitsFunction = new CUnitEnumFunction() {
		@Override
		public boolean call(final CUnit enumUnit) {
			if (hits < maxHits && collisions.get(enumUnit.getHandleId(), 0) < maxHitsPerTarget
					&& enumUnit.canReach(updateLoc, collisionRadius) && canHitTarget(updateGame, enumUnit)) {
				onHitTarget(updateGame, enumUnit);
				if (maxHits > 0) {
					hits++;
				}
				collisions.put(enumUnit.getHandleId(), collisions.get(enumUnit.getHandleId(), 0) + 1);
			}
			return false;
		}
	};

	public CPsuedoProjectile(final float x, final float y, final float speed, final float projectileStepInterval, final int projectileArtSkip,
			final AbilityTarget target, boolean homingEnabled, final CUnit source, final War3ID alias,
			final CEffectType effectType, final int effectArtIndex, final int maxHits, final int hitsPerTarget,
			final float startingRadius, final float finalRadius, final CAbilityCollisionProjectileListener projectileListener, boolean provideCounts) {
		super(x, y, speed, target, homingEnabled, source);
		this.projectileListener = projectileListener;

		this.maxHits = maxHits;
		this.maxHits = maxHits;
		if (this.maxHits <= 0) {
			this.maxHits = 0;
			this.hits = -1;
		}
		
		this.maxHitsPerTarget = hitsPerTarget;
		this.startingCollisionRadius = startingRadius;
		this.finalCollisionRadius = finalRadius;
		this.provideCounts = provideCounts;
		
		this.alias = alias;
		this.effectType = effectType;
		this.effectArtIndex = effectArtIndex;

		final float dtsx = getTargetX() - this.x;
		final float dtsy = getTargetY() - this.y;
		this.distanceToTarget = (float) Math.sqrt((dtsx * dtsx) + (dtsy * dtsy));

		final float d1x = dtsx / this.distanceToTarget;
		final float d1y = dtsy / this.distanceToTarget;
		if (speed != 0) {
			this.steps = ((int)Math.round(this.distanceToTarget) / (int)speed) + 1;
		}
		
		float travelDistance = Math.min(speed, distanceToTarget/this.steps);

		this.dx = d1x * travelDistance;
		this.dy = d1y * travelDistance;

		this.stepInterval = (int) (projectileStepInterval / WarsmashConstants.SIMULATION_STEP_TIME);
		this.artSkip = projectileArtSkip;
	}

	@Override
	protected void onHitTarget(CSimulation game) {
		// Not used
	}

	protected boolean canHitTarget(CSimulation game, CWidget target) {
		return this.projectileListener.canHitTarget(game, target);
	}

	protected void onHitTarget(CSimulation game, CWidget target) {
		projectileListener.onHit(game, this, target);
	}

	@Override
	public boolean update(final CSimulation game) {
		if (this.nextActionTick == 0) {
			this.nextActionTick = game.getGameTurnTick();
		}

		if (game.getGameTurnTick() >= this.nextActionTick) {

			artCount = (artCount+1) % artSkip;
			if (artCount == 0) {
				game.spawnTemporarySpellEffectOnPoint(this.x, this.y, 0, this.alias, this.effectType, this.effectArtIndex);
			}

			if (this.collisionRadius != this.finalCollisionRadius) {
				final float dtsx = getTargetX() - this.x;
				final float dtsy = getTargetY() - this.y;
				final float c = (float) Math.sqrt((dtsx * dtsx) + (dtsy * dtsy));
				this.collisionRadius = this.startingCollisionRadius
						+ (this.finalCollisionRadius - this.startingCollisionRadius)
								* (1 - (c / this.distanceToTarget));
			}
			this.updateGame = game;
			this.updateLoc.x = this.x;
			this.updateLoc.y = this.y;
			this.projectileListener.setCurrentLocation(this.updateLoc);
			recycleRect.set(this.getX() - collisionRadius, this.getY() - collisionRadius, collisionRadius * 2,
					collisionRadius * 2);
			
			if (provideCounts ) {
				this.updateDestCount = 0;
				this.updateUnitCount = 0;
				game.getWorldCollision().enumDestructablesInRect(recycleRect, this.countDestructablesFunction);
				game.getWorldCollision().enumUnitsInRect(recycleRect, this.countUnitsFunction);

				this.projectileListener.setUnitTargets(this.updateUnitCount);
				this.projectileListener.setDestructableTargets(this.updateDestCount);
			}

			
			this.projectileListener.onPreHits(game, this, this.updateLoc);
			
			game.getWorldCollision().enumDestructablesInRect(recycleRect, this.hitDestructablesFunction);
			game.getWorldCollision().enumUnitsInRect(recycleRect, this.hitUnitsFunction);

			this.x = this.x + this.dx;
			this.y = this.y + this.dy;
			
			this.nextActionTick = game.getGameTurnTick() + this.stepInterval;
			this.stepCount++;
			this.done |= this.stepCount > this.steps;
		}
		this.done |= hits >= maxHits;

		return this.done;
	}
}
