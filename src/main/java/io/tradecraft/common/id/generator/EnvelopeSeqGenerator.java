package io.tradecraft.common.id.generator;

import io.tradecraft.common.id.EnvelopeSeq;

public final class EnvelopeSeqGenerator {
    private final IdGenerator<Long> gen;
    public EnvelopeSeqGenerator(IdGenerator<Long> gen) { this.gen = gen; }
    public EnvelopeSeq next() { return new EnvelopeSeq(gen.next()); }
}