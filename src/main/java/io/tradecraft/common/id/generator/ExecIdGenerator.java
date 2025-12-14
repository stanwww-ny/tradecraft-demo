package io.tradecraft.common.id.generator;

import io.tradecraft.common.id.ExecId;

/**
 * Generates venueId-assigned order IDs.
 * <p>
 * In real venues this could be numeric or alphanumeric. For simulation, we use an incrementing long.
 */
public final class ExecIdGenerator {
    private final IdGenerator<String> generator;

    public ExecIdGenerator(IdGenerator<String> generator) {
        this.generator = generator;
    }

    public ExecId next() {
        return new ExecId(generator.next());
    }
}
