package io.tradecraft.sor.handler;

import io.tradecraft.common.domain.validation.OrdTypeValidator;
import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.envelope.EnvelopeMetaFactory;
import io.tradecraft.common.envelope.Meta;
import io.tradecraft.common.envelope.Stage;
import io.tradecraft.common.id.allocator.ChildIdAllocator;
import io.tradecraft.common.id.generator.ChildClOrdIdGenerator;
import io.tradecraft.common.log.LogUtils;
import io.tradecraft.common.spi.oms.intent.ParentCancelIntent;
import io.tradecraft.common.spi.oms.intent.ParentRouteIntent;
import io.tradecraft.common.spi.oms.intent.PubParentIntent;
import io.tradecraft.common.spi.sor.intent.CancelChildIntent;
import io.tradecraft.common.spi.sor.intent.NewChildIntent;
import io.tradecraft.common.spi.sor.intent.PubChildIntent;
import io.tradecraft.oms.event.EventQueue;
import io.tradecraft.sor.routing.VenueRoute;
import io.tradecraft.sor.routing.VenueRoutePlan;
import io.tradecraft.sor.routing.VenueRouter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.tradecraft.common.meta.Component.SOR;
import static io.tradecraft.common.meta.Flow.IN;
import static io.tradecraft.common.meta.Flow.OUT;
import static io.tradecraft.common.meta.MessageType.CMD;

/*
 The Parent layer is the “intent handler” — it decides how many child routes to create, which venues to target, and how to split quantities.
 */
public final class DefaultParentIntentHandler implements ParentIntentHandler {
    private final EventQueue<Envelope<PubChildIntent>> intentBus;
    private final VenueRouter venueRouter;
    private final ChildIdAllocator childIdAllocator;
    private final ChildClOrdIdGenerator childClOrdIdGenerator;
    private final EnvelopeMetaFactory metaFactory;


    public DefaultParentIntentHandler(EventQueue<Envelope<PubChildIntent>> intentBus,
                                      VenueRouter venueRouter,
                                      ChildIdAllocator childIdAllocator,
                                      ChildClOrdIdGenerator childClOrdIdGenerator,
                                      EnvelopeMetaFactory metaFactory) {
        this.intentBus = Objects.requireNonNull(intentBus, "intentBus");
        this.venueRouter = Objects.requireNonNull(venueRouter, "venueRouter");
        this.childIdAllocator = childIdAllocator;
        this.childClOrdIdGenerator = childClOrdIdGenerator;
        this.metaFactory = metaFactory;
    }

    @Override
    public void onIntent(Envelope<PubParentIntent> envelope) {
        PubParentIntent intent = envelope.payload();
        Meta meta = envelope.meta();
        this.metaFactory.addHop(envelope.meta(), Stage.SOR_ROUTE_PARENT);

        LogUtils.log(SOR, CMD, IN, this, intent);
        List<PubChildIntent> childIntents = new ArrayList<>();
        if (intent instanceof ParentRouteIntent ni) {
            childIntents = onRouteIntent(ni);
        } else if (intent instanceof ParentCancelIntent ci) {
            childIntents = onCancel(ci);
        }

        for (PubChildIntent childIntent : childIntents) {
            if (childIntent != null) {
                LogUtils.log(SOR, CMD, OUT, this, childIntent);
                if (childIntent instanceof NewChildIntent) {
                    this.metaFactory.add(envelope.meta(), (NewChildIntent) childIntent);
                }
                this.metaFactory.addHop(envelope.meta(), childIntent);
                intentBus.offer(Envelope.of(childIntent, meta));
            }
        }
    }

    public List<PubChildIntent> onRouteIntent(ParentRouteIntent pi) {

        OrdTypeValidator.validate(pi.ordType(), pi.limitPxMicros());

        long leaves = Math.max(0, pi.leavesQty());
        Long targetVal = pi.targetChildQty();
        long target = (targetVal == null || targetVal <= 0) ? leaves : targetVal;       // ensure ≥ 1
        long childQty = Math.min(leaves, target);
        if (childQty == 0) {
            LogUtils.log(SOR, CMD, IN, this, "No leavesQty to route for parent {}", pi.parentId());
            return null;
        }

        // Choose venueId: directed or default (you can improve with Router later)
        VenueRoutePlan venueRoutePlan = venueRouter.venueRoutePlan(pi);
        List<PubChildIntent> newChildIntents = new ArrayList<>();
        for (VenueRoute venueRoute : venueRoutePlan.routes()) {
            newChildIntents.add(new NewChildIntent(
                    pi.parentId(),
                    childIdAllocator.allocate(),
                    childClOrdIdGenerator.next(),
                    pi.intentId(),
                    pi.accountId(),
                    pi.accountType(),
                    pi.instrumentKey(),
                    pi.side(),
                    venueRoute.qty(),
                    pi.ordType(),
                    pi.limitPxMicros(),
                    pi.tif(),
                    pi.expireAt(),
                    venueRoute.venueId(),
                    pi.tsNanos()
            ));
        }
        return newChildIntents;
    }

    private List<PubChildIntent> onCancel(ParentCancelIntent ci) {
        Objects.requireNonNull(ci.parentId(), "parentId");
        List<PubChildIntent> childIntents = new ArrayList<>();
        childIntents.add(CancelChildIntent.builder()
                .parentId(ci.parentId())
                .childId(ci.childId())
                .childClOrdId(childClOrdIdGenerator.next())
                .instrumentKey(ci.instrumentKey())
                .build());
        return childIntents;
    }
}
