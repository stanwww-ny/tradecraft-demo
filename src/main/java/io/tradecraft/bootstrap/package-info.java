/**
 * Process bootstrap and wiring: launcher entrypoints, module composition, and environment/metrics setup.
 *
 * <h3>Invariants</h3>
 * <ul>
 *   <li>No business logic here; only composition and lifecycle (start/stop).</li>
 *   <li>Resources are {@code AutoCloseable} where applicable.</li>
 *   <li>Configuration is immutable after startup (favor constructor injection).</li>
 * </ul>
 */
@javax.annotation.ParametersAreNonnullByDefault
package io.tradecraft.bootstrap;