package io.tradecraft.venue.cmd;

import io.tradecraft.common.id.ChildClOrdId;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.spi.sor.intent.NewChildIntent;

public class ChildCommandFactory {
    /**
     * Main entry: router decides venueId.
     */
    public NewChildCmd fromIntent(NewChildIntent i,
                                  ChildId childId,
                                  ChildClOrdId childClOrdId,
                                  VenueId venueId) {
        NewChildCmd cmd = new NewChildCmd(
                i.parentId(),
                i.childId(),
                i.childClOrdId(),
                i.accountId(),
                i.accountType(),
                i.instrumentKey(),
                i.side(),
                i.qty(),
                i.ordType(),
                i.priceMicros(),
                i.tif(),
                i.venueId(),
                i.tsNanos()
        );
        return cmd;
    }
}
