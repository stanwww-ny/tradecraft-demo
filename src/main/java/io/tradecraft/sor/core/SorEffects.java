package io.tradecraft.sor.core;

import io.tradecraft.oms.event.OrderEvent;
import io.tradecraft.sor.state.ChildState;
import io.tradecraft.venue.cmd.VenueCommand;

import java.util.List;

public record SorEffects(
        ChildState next,
        List<VenueCommand> venueCommands,  // downstream
        List<OrderEvent>  orderEvents  // upstream
) {
    public static SorEffects of(ChildState next) {
        return new SorEffects(next, List.of(), List.of());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static SorEffects noop() {
        return SorEffects.builder().build();
    }

    public static final class Builder {
        private ChildState next;
        private List<VenueCommand> venueCmds;
        private List<OrderEvent> orderEvents;

        public Builder next(ChildState v) { this.next = v; return this; }
        public Builder venueCmds(List<VenueCommand> v) { this.venueCmds = v; return this; }
        public Builder orderEvents(List<OrderEvent> v) { this.orderEvents = v; return this; }

        public SorEffects build() {
            return new SorEffects(next, venueCmds, orderEvents);
        }
    }
    
}