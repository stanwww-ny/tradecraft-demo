package io.tradecraft.venue.strategy;

import io.tradecraft.venue.api.VenueExecution;
import io.tradecraft.venue.cmd.VenueCommand;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A VenueStrategy both: - Decides if it applies to a given command (via `appliesTo`) - Produces a VenueExecution for
 * that command
 */
public interface VenueStrategy {

    /**
     * Utility to adapt an arbitrary predicate + strategy impl into a VenueStrategy.
     */
    static VenueStrategy of(Predicate<VenueCommand> predicate,
                            Function<VenueCommand, VenueExecution> fn) {
        return new VenueStrategy() {
            @Override
            public boolean appliesTo(VenueCommand command) {
                return predicate.test(command);
            }

            @Override
            public VenueExecution decide(VenueCommand command) {
                return fn.apply(command);
            }
        };
    }

    /**
     * Whether this strategy should handle the given command.
     */
    boolean appliesTo(VenueCommand command);

    /**
     * Execute logic for the command and return the resulting execution bundle.
     */
    VenueExecution decide(VenueCommand command);
}

