package io.tradecraft.venue.api;

import io.tradecraft.common.domain.order.CancelReason;
import io.tradecraft.common.domain.time.DualTimeSource;
import io.tradecraft.common.envelope.EnvelopeMetaFactory;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.ExecId;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.id.allocator.VenueOrderIdAllocator;
import io.tradecraft.common.id.generator.ExecIdGenerator;
import io.tradecraft.venue.cmd.NewChildCmd;
import io.tradecraft.venue.event.VenueAck;
import io.tradecraft.venue.event.VenueCancelDone;
import io.tradecraft.venue.event.VenueFill;
import io.tradecraft.venue.listener.VenueListener;
import io.tradecraft.venue.matching.orderbook.FillSource;
import io.tradecraft.venue.matching.orderbook.RestingRef;
import io.tradecraft.venue.model.VenueOrder;
import io.tradecraft.venue.nbbo.NbboProvider;
import io.tradecraft.venue.store.VenueOrderRepository;

import java.util.Optional;

/**
 * Shared plumbing for venues: - delegates persistence + idempotency to VenueOrderRepository - generates VenueAck /
 * VenueCancel / VenueFill - wires in clocks + metrics + publisher
 */
public final class DefaultVenueSupport implements VenueSupport {
    private final VenueId venueId;
    private final VenueOrderRepository repo;
    private final VenueListener listener;
    private final NbboProvider nbboProvider;
    private final EnvelopeMetaFactory metaFactory;
    private final DualTimeSource dualTimeSource;
    private final VenueOrderIdAllocator venueOrderIdAllocator;
    private final ExecIdGenerator execIdGenerator;

    public DefaultVenueSupport(VenueId venueId, VenueOrderRepository repo, NbboProvider nbboProvider,
                               VenueListener listener, VenueOrderIdAllocator venueOrderIdAllocator,
                               ExecIdGenerator execIdGenerator, EnvelopeMetaFactory metaFactory) {
        this.venueId = venueId;
        this.repo = repo;
        this.listener = listener;
        this.nbboProvider = nbboProvider;
        this.venueOrderIdAllocator = venueOrderIdAllocator;
        this.execIdGenerator = execIdGenerator;
        this.metaFactory = metaFactory;
        this.dualTimeSource = metaFactory.dualTimeSource();
    }

    // VenueContext
    public VenueId venueId() {
        return venueId;
    }

    public NbboProvider nbboProvider() {
        return nbboProvider;
    }

    public VenueOrderRepository orderRepository() {
        return repo;
    }

    public VenueOrderIdAllocator venueOrderIdAllocator() {
        return venueOrderIdAllocator;
    }

    public VenueListener listener() {
        return listener;
    }

    public DualTimeSource dualTimeSource() {
        return dualTimeSource;
    }

    // VenueOrders
    public VenueOrder create(NewChildCmd c) {
        VenueOrder vo = repo.create(c, venueId, venueOrderIdAllocator.allocate());
        repo.ack(vo, dualTimeSource.nowNanos());
        return vo;
    }

    public Optional<VenueOrder> find(ChildId id) {
        return repo.get(id);
    }

    public void markResting(VenueOrder vo, RestingRef resting) {
        repo.markResting(vo, resting);
    }

    public void clearResting(VenueOrder vo) {
        repo.clearResting(vo);
    }

    // VenueEmitter
    public VenueAck ack(NewChildCmd c, VenueOrder vo) {
        return VenueAck.of(vo, ExecId.nextId(), dualTimeSource.nowNanos());
    }

    public VenueCancelDone cancel(VenueOrder vo, CancelReason reason) {
        repo.cancel(vo, reason);
        return VenueCancelDone.builder()
                .childId(vo.childId())
                .venueId(vo.venueId())
                .venueOrderId(vo.venueOrderId())
                .execId(execIdGenerator.next())
                .reason(reason)
                .tsNanos(dualTimeSource.nowNanos())
                .build();
    }

    public VenueCancelDone cancel(VenueOrder vo, long canceledQty, CancelReason reason) {
        repo.cancel(vo, reason);
        // If you have a builder or an overload that takes canceledQty, use it:
        return VenueCancelDone.builder()
                .childId(vo.childId())
                .venueId(vo.venueId())
                .venueOrderId(vo.venueOrderId())
                .execId(execIdGenerator.next())
                .reason(reason)
                .tsNanos(dualTimeSource.nowNanos())
                .canceledQty(canceledQty)   // omit if your model doesn’t have it
                .build();
    }

    public VenueFill applyFill(VenueOrder vo, long lastQty, long lastPxMicros,
                               boolean finalFlag, FillSource src) {
        repo.applyFill(vo, lastQty, lastPxMicros, finalFlag, src);
        VenueFill evt = VenueFill.of(
                vo,
                ExecId.nextId(),
                lastQty,
                lastPxMicros,
                vo.cumQty() + lastQty,  // repo should increment cumQty, adjust if needed
                finalFlag,
                dualTimeSource.nowNanos()
        );
        return evt;
    }

    public void applyReplace(VenueOrder vo, long newQty, Long newLimitPxMicros) {
        repo.applyReplace(vo, newQty, newLimitPxMicros);
        // optional: emit an explicit VenueReplaceAck event if your flow needs it
    }

}
