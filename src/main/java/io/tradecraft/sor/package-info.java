/**
 * Smart Order Router: venueId selection, order slicing, and routing logic.
 *
 * <h3>Invariants</h3>
 * <ul>
 *   <li>Inputs/outputs are OMS-normalized events (no FIX tags).</li>
 *   <li>Deterministic decision functions are pure and unit-testable.</li>
 *   <li>Time/price/qty units match {@code io.tradecraft.common} conventions.</li>
 * </ul>
 *
 * <h3>Subpackages (typical)</h3>
 * <ul>
 *   <li>{@code router}: policies and decision points.</li>
 *   <li>{@code venueId}: adapters & venueId-local events.</li>
 *   <li>{@code calc}: sizing, cost, schedule (VWAP/TWAP) utilities.</li>
 *   <li>{@code api}: intents/commands exposed to OMS.</li>
 * </ul>
 */
@javax.annotation.ParametersAreNonnullByDefault
package io.tradecraft.sor;
