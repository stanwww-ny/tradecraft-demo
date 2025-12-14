// src/main/java/io/tradecraft/sor/policy/VenuePolicy.java
package io.tradecraft.sor.policy;

import io.tradecraft.common.id.VenueId;

public interface VenuePolicy {
    /** Cancel-before-ACK allowed (a.k.a. COA)? */
    boolean supportsCancelBeforeAck(VenueId venueId);

    /** Must the cancel reference a concrete VenueOrderId? (i.e., defer until ACK) */
    boolean requiresVenueOrderIdForCancel(VenueId venueId);
}
