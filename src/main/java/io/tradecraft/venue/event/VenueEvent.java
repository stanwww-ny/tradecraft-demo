package io.tradecraft.venue.event;

import io.tradecraft.common.id.ChildClOrdId;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.id.VenueOrderId;

public sealed interface VenueEvent
        permits VenueAck, VenueCancelAck, VenueCancelDone, VenueCancelReject, VenueFill, VenueNewReject, VenueReject, VenueReplaceAck, VenueReplaceReject {
    VenueId venueId();
    VenueOrderId venueOrderId();
    ChildClOrdId childClOrdId();
}