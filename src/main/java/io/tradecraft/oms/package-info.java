/**
 * Order Management System: normalized events, state machines, and the single-writer pipeline that orchestrates the
 * parent/child order lifecycle.
 *
 * <h3>Invariants</h3>
 * <ul>
 *   <li>Events here are protocol-normalized (no FIX tags).</li>
 *   <li>Timestamps in nanos (UTC), price in micros, qty in integer lots.</li>
 *   <li>All state mutations occur on the pipeline thread (single-writer rule).</li>
 *   <li>Side effects (publishing, routing) are explicit and testable.</li>
 * </ul>
 *
 * <h3>Subpackages (typical)</h3>
 * <ul>
 *   <li>{@code event}: domain events & intents (immutable).</li>
 *   <li>{@code fsm}: parent/child FSMs and effect interfaces.</li>
 *   <li>{@code pipeline}: run loop, guards, and wiring.</li>
 *   <li>{@code runtime}: modules used to bootstrap OMS in-process.</li>
 * </ul>
 */
@javax.annotation.ParametersAreNonnullByDefault
package io.tradecraft.oms;
