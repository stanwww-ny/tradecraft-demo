package io.tradecraft.bootstrap;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.tradecraft.common.domain.time.DualTimeSource;
import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.envelope.EnvelopeMetaFactory;
import io.tradecraft.common.envelope.JdkQueueChannel;
import io.tradecraft.common.id.IdFactory;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.spi.oms.exec.PubExecReport;
import io.tradecraft.common.spi.oms.intent.PubParentIntent;
import io.tradecraft.common.spi.sor.intent.PubChildIntent;
import io.tradecraft.fixqfj.acceptor.OmsFixAcceptor;
import io.tradecraft.fixqfj.acceptor.OmsFixInbound;
import io.tradecraft.fixqfj.session.SessionIndex;
import io.tradecraft.fixqfj.session.SessionResolver;
import io.tradecraft.observability.trace.DefaultTraceWriter;
import io.tradecraft.observability.trace.TraceWriter;
import io.tradecraft.oms.core.DefaultNewStateMapper;
import io.tradecraft.oms.core.NewStateMapper;
import io.tradecraft.oms.core.ParentStateStore;
import io.tradecraft.oms.dispatch.EffectPublisher;
import io.tradecraft.oms.dispatch.InboundDispatcher;
import io.tradecraft.oms.dispatch.QueueManager;
import io.tradecraft.oms.event.EventQueue;
import io.tradecraft.oms.event.OrderEvent;
import io.tradecraft.oms.repo.ClOrdIndex;
import io.tradecraft.oms.repo.ParentFsmRepository;
import io.tradecraft.oms.runtime.ChildFillDeduper;
import io.tradecraft.oms.runtime.DefaultClOrdIndex;
import io.tradecraft.oms.runtime.DefaultEventTranslator;
import io.tradecraft.oms.runtime.DefaultParentFsmExecutor;
import io.tradecraft.oms.runtime.DefaultParentFsmRepository;
import io.tradecraft.oms.runtime.DefaultParentFxProcessor;
import io.tradecraft.oms.runtime.EventTranslator;
import io.tradecraft.oms.runtime.InMemoryParentStateStore;
import io.tradecraft.oms.runtime.ParentCancelRegistry;
import io.tradecraft.oms.runtime.ParentFsmExecutor;
import io.tradecraft.oms.runtime.ParentFxProcessor;
import io.tradecraft.oms.support.StrictThreadGuard;
import io.tradecraft.oms.support.ThreadGuard;
import io.tradecraft.sor.routing.DefaultVenueRouter;
import io.tradecraft.sor.routing.VenueRouter;
import io.tradecraft.venue.VenueFactory;
import io.tradecraft.venue.api.Venue;
import io.tradecraft.venue.event.VenueEvent;
import io.tradecraft.venue.listener.TinyVenueListener;
import io.tradecraft.venue.listener.VenueListener;
import io.tradecraft.venue.nbbo.NbboCache;
import io.tradecraft.venue.registry.DefaultVenueRegistry;
import io.tradecraft.venue.registry.VenueRegistry;
import quickfix.ConfigError;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Bootstrap implements AutoCloseable {
    private final Composite all;

    public Bootstrap(OmsFixAcceptorConfig cfg) throws ConfigError {
        // Queues (still JDK here; easy to swap later)
        EventQueue<Envelope<OrderEvent>> inboundEventBus = new JdkQueueChannel<>();
        EventQueue<Envelope<PubParentIntent>> parentIntentBus = new JdkQueueChannel<>();
        EventQueue<Envelope<OrderEvent>> sorEventBus = new JdkQueueChannel<>();
        EventQueue<Envelope<PubExecReport>> execReportBus = new JdkQueueChannel<>();
        QueueManager queueManager = new QueueManager(inboundEventBus, sorEventBus, execReportBus, parentIntentBus);

        // Trace Writer
        int traceQueueCapacity = 4096;
        String runId = UUID.randomUUID().toString().substring(0, 8);
        TraceWriter erTraceWriter = new DefaultTraceWriter("er", Path.of("trace/er", "er-trace-" + runId + ".jsonl"), traceQueueCapacity);
        TraceWriter internalTraceWriter = new DefaultTraceWriter("internal", Path.of("trace/internal", "internal-trace-" + runId + ".jsonl"), traceQueueCapacity);

        // Ids, Time Source, Envelope
        IdFactory ids = IdFactory.system();
        DualTimeSource dualTimeSource = DualTimeSource.system();
        EnvelopeMetaFactory metaFactory = new EnvelopeMetaFactory(ids.envelopeSeq(), dualTimeSource);

        // Fix Inbound
        var meterRegistry = new SimpleMeterRegistry();
        SessionIndex sessionIndex = new SessionIndex();
        OmsFixInbound fixInbound = new OmsFixInbound(inboundEventBus, metaFactory, sessionIndex, meterRegistry);
        OmsFixAcceptor omsFixAcceptor = new OmsFixAcceptor(cfg, fixInbound);

        // Parent Domain
        ParentStateStore parentStateStore = new InMemoryParentStateStore();
        ClOrdIndex clOrdIndex = new DefaultClOrdIndex();
        ParentFsmRepository parentFsmRepository = new DefaultParentFsmRepository();
        ChildFillDeduper childFillDeduper = new ChildFillDeduper(64);
        ParentCancelRegistry parentCancelRegistry = new ParentCancelRegistry();
        NewStateMapper newStateMapper = new DefaultNewStateMapper();

        // Pipeline
        InboundDispatcher inboundDispatcher = queueManager.inboundDispatcher();
        EffectPublisher effectPublisher = queueManager.publisher(metaFactory);
        ParentFxProcessor parentFxProcessor = new DefaultParentFxProcessor(parentCancelRegistry, queueManager.intentBus());
        EventTranslator eventTranslator = new DefaultEventTranslator(clOrdIndex, ids.parent(), sessionIndex, parentCancelRegistry, metaFactory, childFillDeduper);
        ParentFsmExecutor parentFsmExecutor = new DefaultParentFsmExecutor(parentStateStore, parentFsmRepository, newStateMapper, ids.intent());

        ThreadGuard pipelineGuard = new StrictThreadGuard("pipeline");
        var pipeline = new PipelineModule(
                pipelineGuard, cfg.pipelineThreadName(),
                inboundDispatcher, effectPublisher, eventTranslator, parentFsmExecutor, parentFxProcessor, internalTraceWriter);


        // SOR <--> Venue
        EventQueue<Envelope<VenueEvent>> venueEventBus = new JdkQueueChannel<>();
        EventQueue<Envelope<PubChildIntent>> childIntentBus = new JdkQueueChannel<>();

        // Venue Listener
        VenueListener venueListener = new TinyVenueListener(venueEventBus, metaFactory);

        // NBBO, market data
        NbboCache nbbo = new NbboCache();
        MarketDataModule md = new MarketDataModule(nbbo);
        md.onTopOfBook(/* bid */ 195000000L,  /* ask */ 205000000L, dualTimeSource.nowNanos());

        // Venue
        VenueFactory venueFactory = new VenueFactory();
        Venue xnas = venueFactory.createVenue(VenueId.XNAS, venueListener, ids.venueOrder(), ids.exec(), metaFactory, nbbo, dualTimeSource);
        VenueRegistry venueRegistry = new DefaultVenueRegistry(Map.of(VenueId.XNAS, xnas), VenueId.XNAS);
        VenueRouter venueRouter = new DefaultVenueRouter(venueRegistry, VenueId.XNAS);

        // SOR
        var sor = new SorModule(sorEventBus, parentIntentBus, childIntentBus, venueEventBus, venueRouter, ids.child(), ids.childClOrd(), metaFactory);

        // ExecReport Sender
        SessionResolver prodResolver = er -> {
            var sid = sessionIndex.getByParent(er.parentId());
            return (sid != null) ? sid : omsFixAcceptor.getDefaultSession();
        };
        var er = new ExecReportModule(execReportBus, prodResolver, meterRegistry, metaFactory, erTraceWriter);

        this.all = new Composite(List.of(pipeline, omsFixAcceptor, sor, er));
    }

    public void start() {
        all.start();
    }

    public void stop() {
        all.stop();
    }

    @Override
    public void close() {
        stop();
    }
}