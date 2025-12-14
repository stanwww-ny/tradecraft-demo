package io.tradecraft.venue.api;

import io.tradecraft.venue.event.VenueAck;
import io.tradecraft.venue.event.VenueCancelDone;
import io.tradecraft.venue.event.VenueFill;
import io.tradecraft.venue.event.VenueReject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class VenueExecution {

    private static final VenueExecution NOOP =
            new VenueExecution(List.of(), List.of(), null, null, true);

    private final List<VenueAck> acks;
    private final List<VenueFill> fills;
    private final VenueCancelDone cancel;  // optional
    private final VenueReject reject;  // optional
    private final boolean noop;

    private VenueExecution(List<VenueAck> acks,
                           List<VenueFill> fills,
                           VenueCancelDone cancel,
                           VenueReject reject,
                           boolean noop) {
        this.acks = acks.isEmpty() ? List.of() : List.copyOf(acks);
        this.fills = fills.isEmpty() ? List.of() : List.copyOf(fills);
        this.cancel = cancel;
        this.reject = reject;
        this.noop = noop && this.acks.isEmpty() && this.fills.isEmpty()
                && this.cancel == null && this.reject == null;
    }

    // ---- Factories ----

    public static VenueExecution of(List<VenueAck> acks,
                                    List<VenueFill> fills,
                                    VenueCancelDone cancel,
                                    VenueReject reject) {
        Objects.requireNonNull(acks, "acks");
        Objects.requireNonNull(fills, "fills");
        return new VenueExecution(acks, fills, cancel, reject, false);
    }

    public static VenueExecution events(List<VenueAck> acks, List<VenueFill> fills) {
        return of(acks, fills, null, null);
    }

    public static VenueExecution cancel(VenueCancelDone cancel) {
        Objects.requireNonNull(cancel, "cancel");
        return new VenueExecution(List.of(), List.of(), cancel, null, false);
    }

    public static VenueExecution reject(VenueReject reject) {
        Objects.requireNonNull(reject, "reject");
        return new VenueExecution(List.of(), List.of(), null, reject, false);
    }

    public static VenueExecution noop() {
        return NOOP;
    }

    // ---- Accessors ----

    public boolean isNoop() {
        return noop;
    }

    public List<VenueAck> acks() {
        return acks;
    }

    public List<VenueFill> fills() {
        return fills;
    }

    public Optional<VenueCancelDone> cancelOptional() {
        return Optional.ofNullable(cancel);
    }

    public Optional<VenueReject> rejectOptional() {
        return Optional.ofNullable(reject);
    }

    public boolean isTerminal() {
        return rejectOptional().isPresent() || cancelOptional().isPresent() || !fills().isEmpty();
    }


    // ---- Merge: concatenate lists; right-biased for cancel/reject ----

    public VenueExecution merge(VenueExecution other) {
        if (other == null || other.isNoop()) return this;
        if (this.isNoop()) return other;

        List<VenueAck> mergedAcks = new ArrayList<>(this.acks.size() + other.acks.size());
        mergedAcks.addAll(this.acks);
        mergedAcks.addAll(other.acks);

        List<VenueFill> mergedFills = new ArrayList<>(this.fills.size() + other.fills.size());
        mergedFills.addAll(this.fills);
        mergedFills.addAll(other.fills);

        VenueCancelDone mergedCancel = other.cancel != null ? other.cancel : this.cancel;
        VenueReject mergedReject = other.reject != null ? other.reject : this.reject;

        return VenueExecution.of(mergedAcks, mergedFills, mergedCancel, mergedReject);
    }

    // (equals/hashCode/toString) … unchanged but include reject in comparisons
}
