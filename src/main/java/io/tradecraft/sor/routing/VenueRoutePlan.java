package io.tradecraft.sor.routing;

import io.tradecraft.common.id.IntentId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record VenueRoutePlan(
        IntentId parentIntentId,
        List<VenueRoute> routes
) {

    public static Builder builder(IntentId parentIntentId) {
        return new Builder(parentIntentId);
    }

    public static final class Builder {
        private final IntentId parentIntentId;
        private final List<VenueRoute> routes = new ArrayList<>();

        private Builder(IntentId parentIntentId) {
            this.parentIntentId = Objects.requireNonNull(parentIntentId);
        }

        public Builder addRoute(VenueRoute route) {
            routes.add(Objects.requireNonNull(route));
            return this;
        }

        public VenueRoutePlan build() {
            if (routes.isEmpty()) {
                throw new IllegalStateException("RoutePlan must contain at least one route");
            }
            return new VenueRoutePlan(parentIntentId, List.copyOf(routes));
        }
    }
}

