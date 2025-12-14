package io.tradecraft.common.id.allocator;

import io.tradecraft.common.id.IntentId;
import io.tradecraft.common.id.generator.IdGenerator;

public final class IntentIdAllocator {
    private final IdGenerator<String> generator;

    public IntentIdAllocator(IdGenerator<String> generator) {
        this.generator = generator;
    }

    public IntentId allocate() {
        return new IntentId(generator.next());
    }
}
