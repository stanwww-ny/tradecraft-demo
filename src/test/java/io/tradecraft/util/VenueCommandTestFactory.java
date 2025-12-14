package io.tradecraft.util;

import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.domain.market.DomainOrdType;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.market.DomainTif;
import io.tradecraft.common.domain.time.DualTimeSource;
import io.tradecraft.venue.cmd.NewChildCmd;

public class VenueCommandTestFactory {
    public static NewChildCmd newChildCmd(DomainSide side, DomainOrdType ordType, long pxMicros, DualTimeSource dualTimeSource) {
        return new NewChildCmd(null, null, null,
                null, null, null,
                side, 1000L, ordType, pxMicros, null, null, dualTimeSource.nowNanos());
    }

    public static NewChildCmd newChildCmd(DomainSide side, long qty, DomainOrdType ordType, long pxMicros, DualTimeSource dualTimeSource) {
        return new NewChildCmd(null, null, null,
                null, null, null,
                side, qty, ordType, pxMicros, null, null, dualTimeSource.nowNanos());
    }

    public static NewChildCmd newChildCmd(ChildId childId, DomainSide side, long qty, DomainOrdType ordType, long pxMicros, DomainTif tif, DualTimeSource dualTimeSource) {
        return new NewChildCmd(null, childId, null,
                null, null, null,
                side, qty, ordType, pxMicros, tif, null, dualTimeSource.nowNanos());
    }
}
