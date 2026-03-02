/**
 * <b>Layer: simulation</b> — authoritative game-state simulation: units,
 * abilities, combat, JASS trigger wiring, and the game-turn clock.
 *
 * <h2>Ownership boundary</h2>
 * <ul>
 *   <li>This package owns all mutable game state. Only code inside this
 *       package may write to unit/ability/player data structures.
 *   <li>The simulation layer is <em>render-agnostic</em>: it must not import
 *       LibGDX, OpenGL, or anything under {@code viewer5} except through the
 *       {@code SimulationRenderController} interface.
 *   <li>The simulation layer is <em>network-agnostic</em>: it does not parse
 *       or emit network packets. Turn inputs arrive via {@code GameTurnManager}.
 *   <li>Asset data (unit stats, abilities) is loaded into simulation at
 *       startup through the data-table APIs; the simulation layer does not
 *       hold live references to the asset pipeline.
 * </ul>
 */
package com.etheller.warsmash.viewer5.handlers.w3x.simulation;
