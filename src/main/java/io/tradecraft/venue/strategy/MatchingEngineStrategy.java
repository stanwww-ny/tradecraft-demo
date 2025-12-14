package io.tradecraft.venue.strategy;

import io.tradecraft.venue.api.VenueExecution;
import io.tradecraft.venue.cmd.CancelChildCmd;
import io.tradecraft.venue.cmd.NewChildCmd;
import io.tradecraft.venue.cmd.ReplaceChildCmd;
import io.tradecraft.venue.cmd.VenueCommand;
import io.tradecraft.venue.matching.MatchingEngine;

/**
 * Thin command dispatcher. Keeps orchestration out of the engine.
 * <p>
 * Usage: var strategy = new MatchingEngineStrategy(new MatchingEngine(support)); var exec = strategy.decide(cmd);
 */
public final class MatchingEngineStrategy implements VenueStrategy {
    private final MatchingEngine engine;

    public MatchingEngineStrategy(MatchingEngine engine) {
        this.engine = engine;
    }

    @Override
    public boolean appliesTo(VenueCommand command) {
        // Accepts only the commands that MatchingEngine understands
        return command instanceof NewChildCmd
                || command instanceof CancelChildCmd
                || command instanceof ReplaceChildCmd;
    }


    @Override
    public VenueExecution decide(VenueCommand command) {
        if (command instanceof NewChildCmd n) return engine.onNew(n);
        if (command instanceof CancelChildCmd x) return engine.onCancel(x);
        if (command instanceof ReplaceChildCmd r) return engine.onReplace(r);

        // Should not happen if appliesTo() is respected by caller.
        return VenueExecution.noop();
    }
}
