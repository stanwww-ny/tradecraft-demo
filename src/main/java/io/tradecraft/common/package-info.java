/**
 * Common, reusable types and SPIs shared across the stack.
 *
 * <h3>Invariants</h3>
 * <ul>
 *   <li>Time: {@code long} nanoseconds since epoch (UTC).</li>
 *   <li>Money/Price: integer micros (1e-6) of display currency.</li>
 *   <li>Qty: integer lots (no decimals).</li>
 *   <li>IDs: value types under {@code io.tradecraft.common.domain.id} (never raw strings in APIs).</li>
 *   <li>Nulls: parameters are non-null by default.</li>
 * </ul>
 *
 * <h3>Subpackages (typical)</h3>
 * <ul>
 *   <li>{@code domain.id}: strongly-typed identifiers.</li>
 *   <li>{@code model}: primitive domain objects (side, tif, ordType).</li>
 *   <li>{@code spi}: narrow interfaces for cross-module interaction.</li>
 *   <li>{@code util}: helpers with no runtime state; prefer static methods.</li>
 * </ul>
 */
@javax.annotation.ParametersAreNonnullByDefault
package io.tradecraft.common;
