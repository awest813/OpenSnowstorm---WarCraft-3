package com.etheller.warsmash.viewer5.handlers.w3x.simulation;

import java.util.HashSet;
import java.util.Set;

import com.badlogic.gdx.math.Rectangle;
import com.etheller.warsmash.util.ObjectPool;
import com.etheller.warsmash.util.Quadtree;
import com.etheller.warsmash.util.QuadtreeIntersector;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid.MovementType;

public class CWorldCollision {
	private static final float MINIMUM_COLLISION_SIZE = 0.001f /* THIS IS TO STOP QUADTREE FROM BUSTING */;
	private static final Rectangle tempRect = new Rectangle();
	private final Quadtree<CUnit> deadUnitCollision;
	private final Quadtree<CUnit> groundUnitCollision;
	private final Quadtree<CUnit> airUnitCollision;
	private final Quadtree<CUnit> seaUnitCollision;
	private final Quadtree<CUnit> buildingUnitCollision;
	private final Quadtree<CUnit> anyUnitEnumerableCollision;
	private final Quadtree<CDestructable> destructablesForEnum;
	private final Quadtree<CItem> itemsForEnum;
	private final float maxCollisionRadius;
	private final AnyUnitExceptTwoIntersector anyUnitExceptTwoIntersector;
	private final EachUnitOnlyOnceIntersector eachUnitOnlyOnceIntersector;
	private final DestructableEnumIntersector destructableEnumIntersector;
	private final ItemEnumIntersector itemEnumIntersector;
	private final ItemEnumIntersectorBoolean itemEnumIntersectorBoolean;
	private final ObjectPool<Set<CUnit>> intersectedUnitsSetPool;
	// PERFORMANCE: Cached Rectangle objects to eliminate GC allocations during spatial queries
	private final ObjectPool<Rectangle> rectPool;
	private final ObjectPool<UnitEnumIntersector> unitEnumIntersectorPool;
	private final ObjectPool<UnitInRangeCallback> unitInRangeCallbackPool;
	private final ObjectPool<DestructableInRangeCallback> destructableInRangeCallbackPool;

	public CWorldCollision(final Rectangle entireMapBounds, final float maxCollisionRadius) {
		this.deadUnitCollision = new Quadtree<>(entireMapBounds);
		this.groundUnitCollision = new Quadtree<>(entireMapBounds);
		this.airUnitCollision = new Quadtree<>(entireMapBounds);
		this.seaUnitCollision = new Quadtree<>(entireMapBounds);
		this.buildingUnitCollision = new Quadtree<>(entireMapBounds);
		this.anyUnitEnumerableCollision = new Quadtree<>(entireMapBounds);
		this.destructablesForEnum = new Quadtree<>(entireMapBounds);
		this.itemsForEnum = new Quadtree<>(entireMapBounds);
		this.maxCollisionRadius = maxCollisionRadius;
		this.anyUnitExceptTwoIntersector = new AnyUnitExceptTwoIntersector();
		this.eachUnitOnlyOnceIntersector = new EachUnitOnlyOnceIntersector();
		this.destructableEnumIntersector = new DestructableEnumIntersector();
		this.itemEnumIntersector = new ItemEnumIntersector();
		this.itemEnumIntersectorBoolean = new ItemEnumIntersectorBoolean();
		this.intersectedUnitsSetPool = new ObjectPool<>(32, HashSet::new);
		this.rectPool = new ObjectPool<>(16, Rectangle::new);
		this.unitEnumIntersectorPool = new ObjectPool<>(32, UnitEnumIntersector::new);
		this.unitInRangeCallbackPool = new ObjectPool<>(32, UnitInRangeCallback::new);
		this.destructableInRangeCallbackPool = new ObjectPool<>(32, DestructableInRangeCallback::new);
	}

	public void addUnit(final CUnit unit) {
		Rectangle bounds = unit.getCollisionRectangle();
		if (bounds == null) {
			final float collisionSize = Math.max(MINIMUM_COLLISION_SIZE,
					Math.min(this.maxCollisionRadius, unit.getUnitType().getCollisionSize()));
			bounds = new Rectangle(unit.getX() - collisionSize, unit.getY() - collisionSize, collisionSize * 2,
					collisionSize * 2);
			unit.setCollisionRectangle(bounds);
		}
		if (unit.isBoneCorpse()) {
			this.deadUnitCollision.add(unit, bounds);
		}
		else {
			this.anyUnitEnumerableCollision.add(unit, bounds);
			if (unit.isBuilding()) {
				// buildings are here so that we can include them when enumerating all units in
				// a rect, but they don't really move dynamically, this is kind of pointless
				this.buildingUnitCollision.add(unit, bounds);
			}
			else {
				final MovementType movementType = unit.getMovementType();
				if (movementType != null) {
					switch (movementType) {
					case AMPHIBIOUS:
						this.seaUnitCollision.add(unit, bounds);
						this.groundUnitCollision.add(unit, bounds);
						break;
					case FLOAT:
						this.seaUnitCollision.add(unit, bounds);
						break;
					case FLY:
						this.airUnitCollision.add(unit, bounds);
						break;
					case DISABLED:
						break;
					default:
					case FOOT:
					case FOOT_NO_COLLISION:
					case HORSE:
					case HOVER:
						this.groundUnitCollision.add(unit, bounds);
						break;
					}
				}
			}
		}
	}

	public void addDestructable(final CDestructable dest) {
		final Rectangle bounds = dest.getOrCreateRegisteredEnumRectangle();
		this.destructablesForEnum.add(dest, bounds);
	}

	public void removeDestructable(final CDestructable dest) {
		final Rectangle bounds = dest.getOrCreateRegisteredEnumRectangle();
		this.destructablesForEnum.remove(dest, bounds);
	}

	public void addItem(final CItem item) {
		final Rectangle bounds = item.getOrCreateRegisteredEnumRectangle();
		this.itemsForEnum.add(item, bounds);
	}

	public void removeItem(final CItem item) {
		final Rectangle bounds = item.getOrCreateRegisteredEnumRectangle();
		this.itemsForEnum.remove(item, bounds);
	}

	public void removeUnit(final CUnit unit) {
		final Rectangle bounds = unit.getCollisionRectangle();
		if (bounds != null) {
			this.anyUnitEnumerableCollision.remove(unit, bounds);
			if (unit.isBoneCorpse()) {
				this.deadUnitCollision.remove(unit, bounds);
			}
			else {
				if (unit.isBuilding()) {
					this.buildingUnitCollision.remove(unit, bounds);
				}
				else {
					final MovementType movementType = unit.getMovementType();
					if (movementType != null) {
						switch (movementType) {
						case AMPHIBIOUS:
							this.seaUnitCollision.remove(unit, bounds);
							this.groundUnitCollision.remove(unit, bounds);
							break;
						case FLOAT:
							this.seaUnitCollision.remove(unit, bounds);
							break;
						case FLY:
							this.airUnitCollision.remove(unit, bounds);
							break;
						case DISABLED:
							break;
						default:
						case FOOT:
						case FOOT_NO_COLLISION:
						case HORSE:
						case HOVER:
							this.groundUnitCollision.remove(unit, bounds);
							break;
						}
					}
				}
			}
		}
		unit.setCollisionRectangle(null);
	}

	public void enumUnitsInRect(final Rectangle rect, final CUnitEnumFunction callback) {
		final Set<CUnit> intersectedUnits = this.intersectedUnitsSetPool.acquire();
		intersectedUnits.clear();
		final UnitEnumIntersector intersector = this.unitEnumIntersectorPool.acquire();
		try {
			this.anyUnitEnumerableCollision.intersect(rect, intersector.reset(intersectedUnits, callback));
		}
		finally {
			intersector.clear();
			this.unitEnumIntersectorPool.release(intersector);
			intersectedUnits.clear();
			this.intersectedUnitsSetPool.release(intersectedUnits);
		}
	}

	public void enumCorpsesInRect(final Rectangle rect, final CUnitEnumFunction callback) {
		final Set<CUnit> intersectedUnits = this.intersectedUnitsSetPool.acquire();
		intersectedUnits.clear();
		final UnitEnumIntersector intersector = this.unitEnumIntersectorPool.acquire();
		try {
			this.deadUnitCollision.intersect(rect, intersector.reset(intersectedUnits, callback));
		}
		finally {
			intersector.clear();
			this.unitEnumIntersectorPool.release(intersector);
			intersectedUnits.clear();
			this.intersectedUnitsSetPool.release(intersectedUnits);
		}
	}
	
	public void enumUnitsOrCorpsesInRect(final Rectangle rect, final CUnitEnumFunction callback) {
		final Set<CUnit> intersectedUnits = this.intersectedUnitsSetPool.acquire();
		intersectedUnits.clear();
		final UnitEnumIntersector intersector = this.unitEnumIntersectorPool.acquire();
		try {
			final QuadtreeIntersector<CUnit> intersectorFxn = intersector.reset(intersectedUnits, callback);
			if (!this.anyUnitEnumerableCollision.intersect(rect, intersectorFxn)) {
				this.deadUnitCollision.intersect(rect, intersectorFxn);
			}
		}
		finally {
			intersector.clear();
			this.unitEnumIntersectorPool.release(intersector);
			intersectedUnits.clear();
			this.intersectedUnitsSetPool.release(intersectedUnits);
		}
	}

	public void enumCorpsesInRange(final float x, final float y, final float radius, final CUnitEnumFunction callback) {
		final Rectangle rect = this.rectPool.acquire();
		final UnitInRangeCallback inRangeCallback = this.unitInRangeCallbackPool.acquire();
		try {
			enumCorpsesInRect(rect.set(x - radius, y - radius, radius * 2, radius * 2), inRangeCallback.reset(x, y, radius, callback));
		}
		finally {
			inRangeCallback.clear();
			this.unitInRangeCallbackPool.release(inRangeCallback);
			this.rectPool.release(rect);
		}
	}

	public void enumUnitsInRange(final float x, final float y, final float radius, final CUnitEnumFunction callback) {
		final Rectangle rect = this.rectPool.acquire();
		final UnitInRangeCallback inRangeCallback = this.unitInRangeCallbackPool.acquire();
		try {
			enumUnitsInRect(rect.set(x - radius, y - radius, radius * 2, radius * 2), inRangeCallback.reset(x, y, radius, callback));
		}
		finally {
			inRangeCallback.clear();
			this.unitInRangeCallbackPool.release(inRangeCallback);
			this.rectPool.release(rect);
		}
	}

	public void enumUnitsOrCorpsesInRange(final float x, final float y, final float radius, final CUnitEnumFunction callback) {
		final Rectangle rect = this.rectPool.acquire();
		final UnitInRangeCallback inRangeCallback = this.unitInRangeCallbackPool.acquire();
		try {
			enumUnitsOrCorpsesInRect(rect.set(x - radius, y - radius, radius * 2, radius * 2), inRangeCallback.reset(x, y, radius, callback));
		}
		finally {
			inRangeCallback.clear();
			this.unitInRangeCallbackPool.release(inRangeCallback);
			this.rectPool.release(rect);
		}
	}

	public void enumBuildingsInRect(final Rectangle rect, final QuadtreeIntersector<CUnit> callback) {
		this.buildingUnitCollision.intersect(rect, callback);
	}

	public void enumBuildingsAtPoint(final float x, final float y, final QuadtreeIntersector<CUnit> callback) {
		this.buildingUnitCollision.intersect(x, y, callback);
	}

	public void enumItemsInRect(final Rectangle rect, final CItemEnumFunction callback) {
		this.itemsForEnum.intersect(rect, this.itemEnumIntersector.reset(callback));
	}

	public void enumDestructablesInRect(final Rectangle rect, final CDestructableEnumFunction callback) {
		this.destructablesForEnum.intersect(rect, this.destructableEnumIntersector.reset(callback));
	}

	public void enumDestructablesInRange(final float x, final float y, final float radius,
			final CDestructableEnumFunction callback) {
		final Rectangle rect = this.rectPool.acquire();
		final DestructableInRangeCallback inRangeCallback = this.destructableInRangeCallbackPool.acquire();
		try {
			enumDestructablesInRect(rect.set(x - radius, y - radius, radius * 2, radius * 2), inRangeCallback.reset(x, y, radius, callback));
		}
		finally {
			inRangeCallback.clear();
			this.destructableInRangeCallbackPool.release(inRangeCallback);
			this.rectPool.release(rect);
		}
	}

	public boolean intersectsAnythingOtherThan(final Rectangle newPossibleRectangle, final CUnit sourceUnitToIgnore,
			final MovementType movementType, final boolean forConstruction) {
		return this.intersectsAnythingOtherThan(newPossibleRectangle, sourceUnitToIgnore, null, movementType,
				forConstruction);
	}

	public boolean intersectsAnythingOtherThan(final Rectangle newPossibleRectangle, final CUnit sourceUnitToIgnore,
			final CUnit sourceSecondUnitToIgnore, final MovementType movementType, final boolean forConstruction) {
		if (movementType != null) {
			switch (movementType) {
			case AMPHIBIOUS:
				if (this.seaUnitCollision.intersect(newPossibleRectangle, this.anyUnitExceptTwoIntersector
						.reset(sourceUnitToIgnore, sourceSecondUnitToIgnore, forConstruction))) {
					return true;
				}
				if (this.groundUnitCollision.intersect(newPossibleRectangle, this.anyUnitExceptTwoIntersector
						.reset(sourceUnitToIgnore, sourceSecondUnitToIgnore, forConstruction))) {
					return true;
				}
				return false;
			case FLOAT:
				return this.seaUnitCollision.intersect(newPossibleRectangle, this.anyUnitExceptTwoIntersector
						.reset(sourceUnitToIgnore, sourceSecondUnitToIgnore, forConstruction));
			case FLY:
				return this.airUnitCollision.intersect(newPossibleRectangle, this.anyUnitExceptTwoIntersector
						.reset(sourceUnitToIgnore, sourceSecondUnitToIgnore, forConstruction));
			case FOOT_NO_COLLISION:
				return this.itemsForEnum.intersect(newPossibleRectangle, this.itemEnumIntersectorBoolean);
			case DISABLED:
				return false;
			default:
			case FOOT:
			case HORSE:
			case HOVER:
				return this.groundUnitCollision.intersect(newPossibleRectangle, this.anyUnitExceptTwoIntersector
						.reset(sourceUnitToIgnore, sourceSecondUnitToIgnore, forConstruction));
			}
		}
		return false;
	}

	public void translate(final CUnit unit, final float xShift, final float yShift) {
		final MovementType movementType = unit.getMovementType();
		final Rectangle bounds = unit.getCollisionRectangle();
		if (bounds != null) {
			if (unit.isBoneCorpse()) {
				this.deadUnitCollision.translate(unit, bounds, xShift, yShift);
			}
			else {
				final float oldX = bounds.x;
				final float oldY = bounds.y;
				this.anyUnitEnumerableCollision.translate(unit, bounds, xShift, yShift);
				if (unit.isBuilding()) {
					bounds.x = oldX;
					bounds.y = oldY;
					this.buildingUnitCollision.translate(unit, bounds, xShift, yShift);
				}
				else if (movementType != null) {
					switch (movementType) {
					case AMPHIBIOUS:
						bounds.x = oldX;
						bounds.y = oldY;
						this.seaUnitCollision.translate(unit, bounds, xShift, yShift);
						bounds.x = oldX;
						bounds.y = oldY;
						this.groundUnitCollision.translate(unit, bounds, xShift, yShift);
						break;
					case FLOAT:
						bounds.x = oldX;
						bounds.y = oldY;
						this.seaUnitCollision.translate(unit, bounds, xShift, yShift);
						break;
					case FLY:
						bounds.x = oldX;
						bounds.y = oldY;
						this.airUnitCollision.translate(unit, bounds, xShift, yShift);
						break;
					case DISABLED:
						break;
					default:
					case FOOT:
					case FOOT_NO_COLLISION:
					case HORSE:
					case HOVER:
						bounds.x = oldX;
						bounds.y = oldY;
						this.groundUnitCollision.translate(unit, bounds, xShift, yShift);
						break;
					}
				}
			}
		} // else probably moving a dead unit that isn't corpse yet
	}

	public void translate(final CItem item, final float xShift, final float yShift) {
		final Rectangle bounds = item.getOrCreateRegisteredEnumRectangle();
		if (!item.isDead()) {
			this.itemsForEnum.translate(item, bounds, xShift, yShift);
		}
	}

	private static final class AnyUnitExceptTwoIntersector implements QuadtreeIntersector<CUnit> {
		private CUnit firstUnit;
		private CUnit secondUnit;
		private boolean forConstruction;

		public AnyUnitExceptTwoIntersector reset(final CUnit firstUnit, final CUnit secondUnit,
				final boolean forConstruction) {
			this.firstUnit = firstUnit;
			this.secondUnit = secondUnit;
			this.forConstruction = forConstruction;
			return this;
		}

		@Override
		public boolean onIntersect(final CUnit intersectingObject) {
			if (intersectingObject.isHidden()
					|| MovementType.FOOT_NO_COLLISION.equals(intersectingObject.getMovementType())
					|| (this.forConstruction && intersectingObject.isNoBuildingCollision())
					|| (!this.forConstruction && intersectingObject.isNoUnitCollision())) {
				return false;
			}
			return (intersectingObject != this.firstUnit) && (intersectingObject != this.secondUnit);
		}
	}

	private static final class EachUnitOnlyOnceIntersector implements QuadtreeIntersector<CUnit> {
		private CUnitEnumFunction consumerDelegate;
		private final Set<CUnit> intersectedUnits = new HashSet<>();
		private boolean done;

		public EachUnitOnlyOnceIntersector reset(final CUnitEnumFunction consumerDelegate) {
			this.consumerDelegate = consumerDelegate;
			this.intersectedUnits.clear();
			this.done = false;
			return this;
		}

		@Override
		public boolean onIntersect(final CUnit intersectingObject) {
			if (intersectingObject.isHidden()) {
				return false;
			}
			if (this.done) {
				// This check is because we may use the intersector for multiple intersect
				// calls, see "enumUnitsInRect" and how it uses this intersector first on the
				// ground unit layer, then the flying unit layer, without recycling
				return true;
			}
			if (this.intersectedUnits.add(intersectingObject)) {
				this.done = this.consumerDelegate.call(intersectingObject);
				return this.done;
			}
			return false;
		}
	}

	private static final class DestructableEnumIntersector implements QuadtreeIntersector<CDestructable> {
		private CDestructableEnumFunction consumerDelegate;

		public DestructableEnumIntersector reset(final CDestructableEnumFunction consumerDelegate) {
			this.consumerDelegate = consumerDelegate;
			return this;
		}

		@Override
		public boolean onIntersect(final CDestructable intersectingObject) {
//			if (intersectingObject.isHidden()) { // at time of writing CDestructable did not have isHidden(), uncomment when available
//				return false;
//			}
			return this.consumerDelegate.call(intersectingObject);
		}
	}

	private static final class ItemEnumIntersector implements QuadtreeIntersector<CItem> {
		private CItemEnumFunction consumerDelegate;

		public ItemEnumIntersector reset(final CItemEnumFunction consumerDelegate) {
			this.consumerDelegate = consumerDelegate;
			return this;
		}

		@Override
		public boolean onIntersect(final CItem intersectingObject) {
			if (intersectingObject.isHidden()) {
				return false;
			}
			return this.consumerDelegate.call(intersectingObject);
		}
	}

	private static final class ItemEnumIntersectorBoolean implements QuadtreeIntersector<CItem> {

		@Override
		public boolean onIntersect(final CItem intersectingObject) {
			if (intersectingObject.isHidden()) {
				return false;
			}
			return true;
		}
	}


	private static final class UnitEnumIntersector implements QuadtreeIntersector<CUnit> {
		private Set<CUnit> intersectedUnits;
		private CUnitEnumFunction callback;

		public void clear() {
			this.intersectedUnits = null;
			this.callback = null;
		}

		public UnitEnumIntersector reset(final Set<CUnit> intersectedUnits, final CUnitEnumFunction callback) {
			this.intersectedUnits = intersectedUnits;
			this.callback = callback;
			return this;
		}

		@Override
		public boolean onIntersect(final CUnit unit) {
			if (unit.isHidden() || !this.intersectedUnits.add(unit)) {
				return false;
			}
			return this.callback.call(unit);
		}
	}

	private static final class UnitInRangeCallback implements CUnitEnumFunction {
		private float x;
		private float y;
		private float radius;
		private CUnitEnumFunction callback;

		public void clear() {
			this.callback = null;
		}

		public UnitInRangeCallback reset(final float x, final float y, final float radius, final CUnitEnumFunction callback) {
			this.x = x;
			this.y = y;
			this.radius = radius;
			this.callback = callback;
			return this;
		}

		@Override
		public boolean call(final CUnit enumUnit) {
			if (enumUnit.canReach(this.x, this.y, this.radius)) {
				return this.callback.call(enumUnit);
			}
			return false;
		}
	}

	private static final class DestructableInRangeCallback implements CDestructableEnumFunction {
		private float x;
		private float y;
		private float radius;
		private CDestructableEnumFunction callback;

		public void clear() {
			this.callback = null;
		}

		public DestructableInRangeCallback reset(final float x, final float y, final float radius, final CDestructableEnumFunction callback) {
			this.x = x;
			this.y = y;
			this.radius = radius;
			this.callback = callback;
			return this;
		}

		@Override
		public boolean call(final CDestructable enumUnit) {
			if (enumUnit.distance(this.x, this.y) <= this.radius) {
				return this.callback.call(enumUnit);
			}
			return false;
		}
	}
}