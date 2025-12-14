package io.tradecraft.common.id.allocator;

import io.tradecraft.common.id.ParentId;
import io.tradecraft.common.id.generator.IdGenerator;

public final class ParentIdAllocator {
    private final IdGenerator<String> generator;

    public ParentIdAllocator(IdGenerator<String> generator) {
        this.generator = generator;
    }

    public ParentId allocate() {
        return new ParentId(generator.next());
    }

    @Override
    public String toString() {
        return "ParentIdAllocator[" + generator + "]";
    }
}
