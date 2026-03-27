import re

with open('core/src/com/etheller/warsmash/viewer5/handlers/w3x/simulation/CWorldCollision.java', 'r') as f:
    content = f.read()

# Add clear() to UnitEnumIntersector
content = re.sub(
    r'public UnitEnumIntersector reset\(final Set<CUnit> intersectedUnits, final CUnitEnumFunction callback\) \{',
    r'''public void clear() {
			this.intersectedUnits = null;
			this.callback = null;
		}

		public UnitEnumIntersector reset(final Set<CUnit> intersectedUnits, final CUnitEnumFunction callback) {''',
    content
)

# Add clear() to UnitInRangeCallback
content = re.sub(
    r'public UnitInRangeCallback reset\(final float x, final float y, final float radius, final CUnitEnumFunction callback\) \{',
    r'''public void clear() {
			this.callback = null;
		}

		public UnitInRangeCallback reset(final float x, final float y, final float radius, final CUnitEnumFunction callback) {''',
    content
)

# Add clear() to DestructableInRangeCallback
content = re.sub(
    r'public DestructableInRangeCallback reset\(final float x, final float y, final float radius, final CDestructableEnumFunction callback\) \{',
    r'''public void clear() {
			this.callback = null;
		}

		public DestructableInRangeCallback reset(final float x, final float y, final float radius, final CDestructableEnumFunction callback) {''',
    content
)

# Update finally blocks to call clear()
content = re.sub(
    r'finally \{\n\s*this.unitEnumIntersectorPool.release\(intersector\);',
    r'finally {\n\t\t\tintersector.clear();\n\t\t\tthis.unitEnumIntersectorPool.release(intersector);',
    content
)

content = re.sub(
    r'finally \{\n\s*this.unitInRangeCallbackPool.release\(inRangeCallback\);',
    r'finally {\n\t\t\tinRangeCallback.clear();\n\t\t\tthis.unitInRangeCallbackPool.release(inRangeCallback);',
    content
)

content = re.sub(
    r'finally \{\n\s*this.destructableInRangeCallbackPool.release\(inRangeCallback\);',
    r'finally {\n\t\t\tinRangeCallback.clear();\n\t\t\tthis.destructableInRangeCallbackPool.release(inRangeCallback);',
    content
)

with open('core/src/com/etheller/warsmash/viewer5/handlers/w3x/simulation/CWorldCollision.java', 'w') as f:
    f.write(content)
