package io.tradecraft.util.sample;

import io.tradecraft.common.domain.market.DomainAccountType;
import io.tradecraft.common.domain.market.DomainOrdType;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.market.DomainTif;
import io.tradecraft.common.id.ClOrdId;
import io.tradecraft.common.id.ParentId;
import io.tradecraft.fixqfj.session.SessionKey;
import io.tradecraft.oms.event.EvBoundParentNew;
import io.tradecraft.oms.event.EvNew;

public class OrderEventSamples {
    static ParentId parentId = ParentId.of("PARENT_ID");
    public static EvBoundParentNew evBoundParentNew() {
        return new EvBoundParentNew(
                parentId,          // or however ParentId is constructed
                System.nanoTime(),
                new SessionKey("beginString", "senderCompID", "targetCompID", "sessionQualifier"),      // or your real constructor
                new ClOrdId("CL001"),
                "ACC1",
                DomainAccountType.CUSTOMER,        // adjust to real enum/value
                InstrumentKeySamples.AAPL,     // or appropriate key
                DomainSide.BUY,
                100,
                DomainOrdType.LIMIT,
                100_000L,                      // limit price micros
                DomainTif.DAY,
                "XNAS"
        );
    }

    public static EvNew evNew() {
        return new EvNew(
                parentId,          // or however ParentId is constructed
                System.nanoTime(),
                ClOrdIdSamples.CL_ORD_ID_001,
                AccountSamples.ACC1,
                AccountSamples.ACC1_TYPE,
                InstrumentKeySamples.AAPL,
                DomainSide.BUY,
                1_000L,
                DomainOrdType.MARKET,
                null,                  // limitPxMicros
                DomainTif.DAY,
                ExDestSamples.XNYS
        );
    }
}
