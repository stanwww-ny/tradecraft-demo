package io.tradecraft.common.id.generator;

import io.tradecraft.common.id.MatchingEngineSeq;

public final class MatchingEngineSeqGenerator {
    private final IdGenerator<Long> gen;
    public MatchingEngineSeqGenerator(IdGenerator<Long> gen) { this.gen = gen; }
    public MatchingEngineSeq next() { return new MatchingEngineSeq(gen.next()); }
}
