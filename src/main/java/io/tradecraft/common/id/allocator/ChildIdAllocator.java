package io.tradecraft.common.id.allocator;

import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.generator.IdGenerator;

public final class ChildIdAllocator {
    private final IdGenerator<String> generator;

    public ChildIdAllocator(IdGenerator<String> generator) {
        this.generator = generator;
    }

    public ChildId allocate() {
        return new ChildId(generator.next());
    }

    @Override
    public String toString() {
        return "ChildIdAllocator[" + generator + "]";
    }
}
