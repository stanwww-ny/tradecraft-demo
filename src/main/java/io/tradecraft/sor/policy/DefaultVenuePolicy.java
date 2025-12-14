// src/main/java/io/tradecraft/sor/policy/DefaultVenuePolicy.java
package io.tradecraft.sor.policy;

import io.tradecraft.common.id.VenueId;
import java.util.Map;
import java.util.Objects;

public final class DefaultVenuePolicy implements VenuePolicy {
    public record Rules(
            boolean supportsCancelBeforeAck,
            boolean requiresVenueOrderIdForCancel
    ) {
        public static Rules COA()   { return new Rules(true,  false); }
        public static Rules STRICT(){ return new Rules(false, true); }
    }

    private final Map<VenueId, Rules> byVenue;
    private final Rules defaults;

    public DefaultVenuePolicy(Map<VenueId, Rules> byVenue, Rules defaults) {
        this.byVenue = Objects.requireNonNull(byVenue);
        this.defaults = Objects.requireNonNull(defaults);
    }

    private Rules r(VenueId v) { return byVenue.getOrDefault(v, defaults); }

    @Override public boolean supportsCancelBeforeAck(VenueId v) {
        return r(v).supportsCancelBeforeAck();
    }

    @Override public boolean requiresVenueOrderIdForCancel(VenueId v) {
        return r(v).requiresVenueOrderIdForCancel();
    }
}
