/**
 * <b>Layer: assets</b> — virtual filesystem and asset-loading pipeline.
 *
 * <p>Provides a unified {@code DataSource} abstraction over multiple storage
 * backends: bare filesystem directories, MPQ archives (Warcraft III 1.22–1.28),
 * and CASC storage (Reforged 1.32.x). Callers obtain an {@code InputStream}
 * for any path without knowing which backend serves it.
 *
 * <h2>Ownership boundary</h2>
 * <ul>
 *   <li>The assets layer has no dependency on rendering, simulation, or
 *       networking. It is a pure I/O abstraction.
 *   <li>All other layers depend <em>downward</em> onto this package; this
 *       package depends on nothing above it.
 *   <li>Asset discovery logic (path resolution, MPQ/CASC mounting) belongs
 *       here. Format parsing (MDX, BLP, SLK) belongs in the callers.
 * </ul>
 */
package com.etheller.warsmash.datasources;
