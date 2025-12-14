package io.tradecraft.common.envelope;

import io.tradecraft.common.domain.time.DualTimeSource;
import io.tradecraft.common.id.generator.EnvelopeSeqGenerator;
import io.tradecraft.common.spi.oms.exec.PubExecReport;
import io.tradecraft.common.spi.oms.intent.ParentCancelIntent;
import io.tradecraft.common.spi.oms.intent.ParentRouteIntent;
import io.tradecraft.common.spi.oms.intent.PubParentIntent;
import io.tradecraft.common.spi.sor.intent.CancelChildIntent;
import io.tradecraft.common.spi.sor.intent.NewChildIntent;
import io.tradecraft.common.spi.sor.intent.PubChildIntent;
import io.tradecraft.oms.core.OrderStatus;
import io.tradecraft.oms.event.EvNew;
import io.tradecraft.venue.cmd.CancelChildCmd;
import io.tradecraft.venue.cmd.NewChildCmd;
import io.tradecraft.venue.cmd.ReplaceChildCmd;
import io.tradecraft.venue.cmd.VenueCommand;

import static io.tradecraft.common.envelope.Stage.ER_RECV_ACK;
import static io.tradecraft.common.envelope.Stage.ER_RECV_CANCELLED;
import static io.tradecraft.common.envelope.Stage.ER_RECV_FILLED;
import static io.tradecraft.common.envelope.Stage.OMS_CANCEL_PARENT;
import static io.tradecraft.common.envelope.Stage.OMS_PUB_ACKED;
import static io.tradecraft.common.envelope.Stage.OMS_PUB_CANCEL;
import static io.tradecraft.common.envelope.Stage.OMS_PUB_FILLED;
import static io.tradecraft.common.envelope.Stage.OMS_ROUTE_PARENT;
import static io.tradecraft.common.envelope.Stage.SOR_ROUTE_CHILD;
import static io.tradecraft.common.envelope.Stage.SOR_SEND_CANCEL_ORDER;
import static io.tradecraft.common.envelope.Stage.UNKNOWN;
import static io.tradecraft.common.envelope.Stage.VENUE_RECV_CANCEL_ORDER;
import static io.tradecraft.common.envelope.Stage.VENUE_RECV_ORDER;
import static io.tradecraft.common.envelope.Stage.VENUE_RECV_REPLACE_ORDER;

public final class EnvelopeMetaFactory {

    private final EnvelopeSeqGenerator seqGen;
    private final DualTimeSource dualTimeSource;

    public EnvelopeMetaFactory(EnvelopeSeqGenerator seqGen, DualTimeSource dualTimeSource) {
        this.seqGen = seqGen;
        this.dualTimeSource = dualTimeSource;
    }

    public EnvelopeMeta newMeta() {
        long seq = seqGen.next().value();   // or next() if you keep it as long
        long nano = dualTimeSource.nowNanos();
        long wall = dualTimeSource.wallClockMillis();

        return EnvelopeMeta.builder()
                .schemaVersion(1)
                .seq(seq)
                .createdNano(nano)
                .createdWallMillis(wall)
                .lastTouchedNano(nano)
                // hops = empty list initially
                .build();
    }

    public EnvelopeMeta copy(Meta meta) {
        EnvelopeMeta envelopeMeta = (EnvelopeMeta) meta;
        return envelopeMeta.copy().build();
    }

    public EnvelopeMeta touch(EnvelopeMeta previous) {
        // Optionally update lastTouched/hops here
        previous.lastTouchedNano = dualTimeSource.nowNanos();
        return previous;
    }

    public DualTimeSource dualTimeSource() {
        return dualTimeSource;
    }

    public void add(Meta meta, EvNew evNew) {
        EnvelopeMeta envelopeMeta = (EnvelopeMeta) meta;
        envelopeMeta.parentId = evNew.parentId();
        envelopeMeta.clOrdId = evNew.clOrdId();
    }

    public void add(Meta meta, NewChildIntent newChildIntent) {
        EnvelopeMeta envelopeMeta = (EnvelopeMeta) meta;
        envelopeMeta.childId = newChildIntent.childId();
    }

    public void addHop(Meta meta, Stage stage) {
        EnvelopeMeta envelopeMeta = (EnvelopeMeta) meta;

        envelopeMeta.addHop(stage, dualTimeSource.nowNanos());
    }

    public void addHop(Meta meta, Stage stage, long nanos) {
        EnvelopeMeta envelopeMeta = (EnvelopeMeta) meta;
        envelopeMeta.addHop(stage, nanos);
    }

    public void addHop(Meta meta, PubParentIntent pubParentIntent) {
        EnvelopeMeta envelopeMeta = (EnvelopeMeta) meta;
        long nanos = dualTimeSource.nowNanos();
        Stage stage = UNKNOWN;
        if (pubParentIntent instanceof ParentRouteIntent) {
            stage = OMS_ROUTE_PARENT;
        }
        if (pubParentIntent instanceof ParentCancelIntent) {
            stage = OMS_CANCEL_PARENT;
        }
        envelopeMeta.addHop(stage, nanos);
    }

    public void addHop(Meta meta, PubChildIntent pubChildIntent) {
        EnvelopeMeta envelopeMeta = (EnvelopeMeta) meta;
        long nanos = dualTimeSource.nowNanos();
        Stage stage = UNKNOWN;
        if (pubChildIntent instanceof NewChildIntent) {
            stage = SOR_ROUTE_CHILD;
        }
        if (pubChildIntent instanceof CancelChildIntent) {
            stage = SOR_SEND_CANCEL_ORDER;
        }
        envelopeMeta.addHop(stage, nanos);
    }

    public void addHop(Meta meta, VenueCommand venueCommand) {
        EnvelopeMeta envelopeMeta = (EnvelopeMeta) meta;
        long nanos = dualTimeSource.nowNanos();
        Stage stage = UNKNOWN;
        if (venueCommand instanceof NewChildCmd) {
            stage = VENUE_RECV_ORDER;
        }
        if (venueCommand instanceof CancelChildCmd) {
            stage = VENUE_RECV_CANCEL_ORDER;
        }
        if (venueCommand instanceof ReplaceChildCmd) {
            stage = VENUE_RECV_REPLACE_ORDER;
        }
        envelopeMeta.addHop(stage, nanos);
    }

    public void addHop(Meta meta, PubExecReport er) {
        EnvelopeMeta envelopeMeta = (EnvelopeMeta) meta;
        long nanos = dualTimeSource.nowNanos();
        Stage stage = UNKNOWN;
        if (er.status() == OrderStatus.WORKING) {
            stage = OMS_PUB_ACKED;
        }
        if (er.status() == OrderStatus.FILLED) {
            stage = OMS_PUB_FILLED;
        }
        if (er.status() == OrderStatus.CANCELED) {
            stage = OMS_PUB_CANCEL;
        }
        envelopeMeta.addHop(stage, nanos);
    }

    public void addErHop(Meta meta, PubExecReport er) {
        EnvelopeMeta envelopeMeta = (EnvelopeMeta) meta;
        long nanos = dualTimeSource.nowNanos();
        Stage stage = UNKNOWN;
        if (er.status() == OrderStatus.WORKING) {
            stage = ER_RECV_ACK;
        }
        if (er.status() == OrderStatus.FILLED) {
            stage = ER_RECV_FILLED;
        }
        if (er.status() == OrderStatus.CANCELED) {
            stage = ER_RECV_CANCELLED;
        }
        envelopeMeta.addHop(stage, nanos);
    }
}

