/**
 * QuickFIX/J integration: transport wiring, sessions, and adapters that translate between FIX and domain concepts.
 *
 * <h3>Boundaries</h3>
 * <ul>
 *   <li>No business logic or domain FSMs here.</li>
 *   <li>Only FIX-facing concerns: acceptor/initiator, sessions, codecs, mappers.</li>
 *   <li>Do not leak FIX tags beyond this package; use normalized events in OMS.</li>
 * </ul>
 *
 * <h3>Invariants</h3>
 * <ul>
 *   <li>All FIX timestamps converted to epoch nanos (UTC) at boundaries.</li>
 *   <li>Map price to micros and qty to integer lots on ingress.</li>
 *   <li>Session identity accessed via typed keys (no raw strings).</li>
 * </ul>
 *
 * <h3>Subpackages (typical)</h3>
 * <ul>
 *   <li>{@code server}: acceptor/initiator wrappers.</li>
 *   <li>{@code session}: session index/resolver and lifecycle handlers.</li>
 *   <li>{@code inbound}/{@code outbound}: FIX↔domain mappers & bridges.</li>
 * </ul>
 */
@javax.annotation.ParametersAreNonnullByDefault
package io.tradecraft.fixqfj;
