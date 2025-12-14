package io.tradecraft.common.id;

import io.tradecraft.common.id.allocator.ChildIdAllocator;
import io.tradecraft.common.id.allocator.ParentIdAllocator;
import io.tradecraft.common.id.allocator.IntentIdAllocator;
import io.tradecraft.common.id.allocator.VenueOrderIdAllocator;
import io.tradecraft.common.id.generator.ChildClOrdIdGenerator;
import io.tradecraft.common.id.generator.EnvelopeSeqGenerator;
import io.tradecraft.common.id.generator.ExecIdGenerator;
import io.tradecraft.common.id.generator.MatchingEngineSeqGenerator;
import io.tradecraft.common.id.generator.MonotonicLongGenerator;
import io.tradecraft.common.id.generator.SplitMixIdGenerator;

/**
 * Central factory that wires all domain ID allocators/generators. - Prod: use IdFactory.system() → nodeSalt +
 * bootSeedSecure() - Replay/Test: use IdFactory.replay(nodeSalt, bootSeed) or IdFactory.testIds(seed)
 * <p>
 * IMPORTANT: SplitMix generators must incorporate seedSalt in output (i.e., mix(counter ^ seedSalt)) so different
 * nodes/boots are disjoint.
 */
public final class IdFactory {

    // Domain-specific allocators/generators (public fields for easy DI)
    final ParentIdAllocator parent;
    final ChildIdAllocator child;
    final VenueOrderIdAllocator venueOrder;
    final IntentIdAllocator intent;
    final ChildClOrdIdGenerator childClOrd;
    final ExecIdGenerator exec;
    final EnvelopeSeqGenerator envelopeSeq;
    final MatchingEngineSeqGenerator meSeq;
    /**
     * Primary constructor for **replayable** runs: pass explicit salts.
     *
     * @param nodeSalt stable per-node value (e.g., StableIds.nodeSalt())
     * @param bootSeed fresh per-boot value (e.g., StableIds.bootSeedSecure())
     */
    public IdFactory(long nodeSalt, long bootSeed) {
        long base = nodeSalt ^ bootSeed; // disjoint per node x boot

        // Distinct domain salts (hash the labels to avoid accidental overlap)
        long P = domainSalt(base, "PARENT");
        long C = domainSalt(base, "CHILD");
        long V = domainSalt(base, "VENUE_ORDER");
        long IN = domainSalt(base, "INTENT");
        long CC = domainSalt(base, "CHILD_CL_ORD");
        long EX = domainSalt(base, "EXEC");

        // Start counters at 0; salt makes streams disjoint
        this.parent = new ParentIdAllocator(new SplitMixIdGenerator("PO", 0L, P));
        this.child = new ChildIdAllocator(new SplitMixIdGenerator("CO", 0L, C));
        this.venueOrder = new VenueOrderIdAllocator(new SplitMixIdGenerator("VO", 0L, V));
        this.intent = new IntentIdAllocator(new SplitMixIdGenerator("IN", 0L, IN));
        this.childClOrd = new ChildClOrdIdGenerator(new SplitMixIdGenerator("CC", 0L, CC));
        this.exec = new ExecIdGenerator(new SplitMixIdGenerator("EX", 0L, EX));

        this.envelopeSeq = new EnvelopeSeqGenerator(
                new MonotonicLongGenerator(0L)
        );
        this.meSeq = new MatchingEngineSeqGenerator(
                new MonotonicLongGenerator(0L)
        );
    }

    /**
     * Convenience: production bootstrap (new bootSeed every start).
     */
    public static IdFactory system() {
        long nodeSalt = StableIds.nodeSalt();
        long bootSeed = StableIds.bootSeedSecure();
        return new IdFactory(nodeSalt, bootSeed);
    }

    /**
     * Deterministic factory for tests: single seed expanded to salts.
     */
    public static IdFactory testIds(long seed) {
        // Use seed both as nodeSalt and bootSeed to get a stable base
        return new IdFactory(seed ^ 0x9e3779b97f4a7c15L, seed);
    }

    /**
     * Replay constructor when you loaded previously recorded salts.
     */
    public static IdFactory replay(long nodeSalt, long bootSeed) {
        return new IdFactory(nodeSalt, bootSeed);
    }

    private static long domainSalt(long base, String label) {
        // simple 64-bit hash of label, xored with base, with some mixing
        long h = 0x9e3779b97f4a7c15L;
        for (int i = 0; i < label.length(); i++) {
            h ^= label.charAt(i);
            h *= 0xff51afd7ed558ccdL;
            h ^= (h >>> 33);
        }
        h ^= base;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= (h >>> 33);
        return h;
    }

    // Optional getters if you prefer methods over public fields
    public ParentIdAllocator parent() {
        return parent;
    }

    public ChildIdAllocator child() { return child; }

    public VenueOrderIdAllocator venueOrder() {
        return venueOrder;
    }

    public IntentIdAllocator intent() {
        return intent;
    }

    public ChildClOrdIdGenerator childClOrd() {
        return childClOrd;
    }

    public ExecIdGenerator exec() {
        return exec;
    }

    public EnvelopeSeqGenerator envelopeSeq() { return envelopeSeq; }

    public MatchingEngineSeqGenerator meSeq() { return meSeq; }
}
