/**
 * <b>Layer: render</b> — OpenGL rendering, MDX/W3X model loading, scene
 * management, and shader infrastructure.
 *
 * <h2>Ownership boundary</h2>
 * <ul>
 *   <li>This package and its sub-packages own all rendering concerns: scene
 *       graph, model instances, textures, shaders, and the GPU upload path.
 *   <li>Code in the {@code render} layer may <em>read</em> simulation state
 *       (positions, animation sequences) but must <em>never</em> mutate it.
 *   <li>Upward dependency on the {@code simulation} layer is forbidden; the
 *       render layer receives simulation state through the
 *       {@code SimulationRenderController} interface.
 *   <li>Downward dependency on the {@code assets} layer (datasources) is
 *       permitted for loading model and texture data.
 * </ul>
 */
package com.etheller.warsmash.viewer5;
