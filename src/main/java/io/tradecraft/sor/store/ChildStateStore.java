package io.tradecraft.sor.store;

import io.tradecraft.common.id.ChildClOrdId;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.id.VenueOrderId;
import io.tradecraft.common.spi.sor.intent.PubChildIntent;
import io.tradecraft.sor.core.SorEffects;
import io.tradecraft.sor.state.ChildState;
import io.tradecraft.venue.event.VenueEvent;

import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public interface ChildStateStore {
    ChildState get(ChildId id);
    ChildState upsert(ChildId id, Supplier<ChildState> create);
    ChildState update(ChildId id, UnaryOperator<ChildState> fn);
    <T extends PubChildIntent> SorEffects apply(ChildId id, T intent,
                     BiFunction<ChildState, T, SorEffects> reducer);

    <T extends VenueEvent> SorEffects apply(VenueId venueId, VenueOrderId venueOrderId,
                                            ChildClOrdId childClOrdId, T venueEvent,
                                            BiFunction<ChildState, T, SorEffects> reducer);
}
