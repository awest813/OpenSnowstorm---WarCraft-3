/**
 * <b>Layer: net (client-side)</b> — client networking protocol, turn-delivery
 * guarantees, and lobby state synchronisation.
 *
 * <p>Contains the client-side half of the Warsmash network protocol. The
 * authoritative server implementation lives in the {@code server} Gradle
 * subproject under {@code com.etheller.warsmash.networking.uberserver}.
 *
 * <h2>Ownership boundary</h2>
 * <ul>
 *   <li>The net layer serialises and deserialises turn commands; it does not
 *       interpret or execute them — that is the simulation layer's job.
 *   <li>The net layer must not import rendering or asset-loading code.
 *   <li>Turn commands received from the server are handed to
 *       {@code GameTurnManager} which feeds them into the simulation loop at
 *       the correct tick boundary.
 * </ul>
 */
package com.etheller.warsmash.networking;
