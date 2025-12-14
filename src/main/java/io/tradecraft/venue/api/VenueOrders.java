package io.tradecraft.venue.api;

import io.tradecraft.common.id.ChildId;
import io.tradecraft.venue.cmd.NewChildCmd;
import io.tradecraft.venue.matching.orderbook.RestingRef;
import io.tradecraft.venue.model.VenueOrder;

import java.util.Optional;

// Order factory/CRUD helpers (optional, used by controllers/engine)
public interface VenueOrders {
    VenueOrder create(NewChildCmd c);     // build + persist + return

    Optional<VenueOrder> find(ChildId id);

    void markResting(VenueOrder vo, RestingRef ref);

    void clearResting(VenueOrder vo);
}