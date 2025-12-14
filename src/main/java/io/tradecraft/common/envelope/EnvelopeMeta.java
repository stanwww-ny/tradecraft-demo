package io.tradecraft.common.envelope;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.ClOrdId;
import io.tradecraft.common.id.ParentId;
import io.tradecraft.common.log.LogUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.tradecraft.common.meta.Component.COMMON;
import static io.tradecraft.common.meta.Flow.NA;
import static io.tradecraft.common.meta.MessageType.ADMIN;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public final class EnvelopeMeta implements Meta, Serializable {
    private final int schemaVersion;
    public ParentId parentId;
    public ChildId childId;
    public ClOrdId clOrdId;
    public final long createdNano;
    public final long createdWallMillis;
    public final long seq;
    public boolean sealed;

    /** Last time this envelope was touched (updated at each hop). */
    public volatile long lastTouchedNano;

    /** Immutable list reference; internal elements can be appended. */
    private final List<HopStamp> hops;

    private EnvelopeMeta(Builder builder) {
        this.schemaVersion = builder.schemaVersion;
        this.createdNano = builder.createdNano;
        this.createdWallMillis = builder.createdWallMillis;
        this.seq = builder.seq;

        this.parentId = builder.parentId;
        this.childId = builder.childId;
        this.clOrdId = builder.clOrdId;

        this.lastTouchedNano = builder.lastTouchedNano;
        this.hops = new ArrayList<>(builder.hops); // already new ArrayList in builder
        this.sealed = false;
    }

    public int schemaVersion() { return schemaVersion; }
    public long createdNano() { return createdNano; }
    public long createdWallMillis() { return createdWallMillis; }
    public long seq() { return seq; }
    public ParentId parentId() { return parentId; }
    public ChildId childId() { return childId; }
    public ClOrdId clOrdId() { return clOrdId; }
    public long lastTouchedNano() { return lastTouchedNano; }

    /** Returns an unmodifiable view for safety. */
    public List<HopStamp> hops() { return Collections.unmodifiableList(hops); }

    public void sealed() {
        sealed = true;
    }
    public void addHop(Stage hopId, long stageNanos) {
        if (sealed) {
            LogUtils.log(COMMON, ADMIN, NA, this, "Cannot add hop {} to a sealed envelope", hopId);
            return;
        }
        hops.add(HopStamp.builder().stage(hopId).stageNanos(stageNanos).build());
        lastTouchedNano = stageNanos;
    }

    public Builder copy() {
        Builder builder = new Builder()
                .schemaVersion(schemaVersion)
                .createdNano(createdNano)
                .createdWallMillis(createdWallMillis)
                .seq(seq)
                .parentId(parentId)
                .childId(childId)
                .clOrdId(clOrdId)
                .lastTouchedNano(lastTouchedNano);
        builder.hops.addAll(this.hops);
        return builder;
    }

    public EnvelopeMeta forPubEr(long publishNano) {
        Builder builder = new Builder()
                .schemaVersion(schemaVersion)
                .createdNano(publishNano)
                .createdWallMillis(createdWallMillis)
                .seq(seq)
                .parentId(parentId)
                .childId(childId)
                .clOrdId(clOrdId);
        builder.lastTouchedNano(publishNano);
        builder.hops = new ArrayList<>();
        return builder.build();
    }

    public EnvelopeMeta copyWithoutHops() {
        return EnvelopeMeta.builder()
                .schemaVersion(this.schemaVersion)
                .createdNano(this.createdNano)
                .createdWallMillis(this.createdWallMillis)
                .seq(this.seq)
                .lastTouchedNano(this.lastTouchedNano)
                .parentId(this.parentId)
                .childId(this.childId)
                .clOrdId(this.clOrdId)
                // hops omitted: builder starts with an empty list
                .build();
    }

    // ---------- Builder ----------
    public static final class Builder {
        private int schemaVersion = 1;
        private long createdNano;
        private long createdWallMillis;
        private long seq;
        private long lastTouchedNano;
        private ParentId parentId;
        private ChildId childId;
        private ClOrdId clOrdId;

        /** backing storage, mutable, never null */
        private List<HopStamp> hops = new ArrayList<>();

        public Builder copy() {
            return new Builder()
                    .parentId(this.parentId)
                    .clOrdId(this.clOrdId)
                    .childId(this.childId);
        }

        public Builder schemaVersion(int schemaVersion) { this.schemaVersion = schemaVersion; return this; }
        public Builder createdNano(long createdNano) { this.createdNano = createdNano; return this; }
        public Builder createdWallMillis(long createdWallMillis) { this.createdWallMillis = createdWallMillis; return this; }
        public Builder lastTouchedNano(long lastTouchedNano) { this.lastTouchedNano = lastTouchedNano; return this; }
        public Builder seq(long seq) { this.seq = seq; return this; }
        public Builder parentId(ParentId parentId) {
            if (this.parentId == null) { this.parentId = parentId; }
            return this;
        }
        public Builder childId(ChildId childId) {
            if (this.childId == null) { this.childId = childId; }
            return this;
        }
        public Builder clOrdId(ClOrdId clOrdId) {
            if (this.clOrdId == null) { this.clOrdId = clOrdId; }
            return this;
        }

        public EnvelopeMeta build() {
            return new EnvelopeMeta(this);
        }
    }

    // Factory
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "EnvelopeMeta{" +
                "schemaVersion=" + schemaVersion +
                ", createdNano=" + createdNano +
                ", createdWallMillis=" + createdWallMillis +
                ", seq=" + seq +
                ", lastTouchedNano=" + lastTouchedNano +
                ", hops=" + hops +
                '}';
    }
}
