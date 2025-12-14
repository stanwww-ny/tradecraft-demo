package io.tradecraft.common.id.generator;

import io.tradecraft.common.id.ChildClOrdId;

public final class ChildClOrdIdGenerator {
    private final IdGenerator<String> generator;

    public ChildClOrdIdGenerator(IdGenerator<String> generator) {
        this.generator = generator;
    }

    public ChildClOrdId next() {
        return new ChildClOrdId(generator.next());
    }
}
