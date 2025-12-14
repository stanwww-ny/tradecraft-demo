// src/test/java/org/example/v4/bootstrap/SorModuleTest.java
package io.tradecraft.bootstrap;

import io.tradecraft.common.domain.time.DualTimeSource;
import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.envelope.EnvelopeMetaFactory;
import io.tradecraft.common.envelope.JdkQueueChannel;
import io.tradecraft.common.id.IdFactory;
import io.tradecraft.common.id.generator.EnvelopeSeqGenerator;
import io.tradecraft.common.spi.oms.intent.PubParentIntent;
import io.tradecraft.common.spi.sor.intent.PubChildIntent;
import io.tradecraft.common.testing.TestClocks;
import io.tradecraft.oms.event.EventQueue;
import io.tradecraft.oms.event.OrderEvent;
import io.tradecraft.sor.routing.VenueRouter;
import io.tradecraft.venue.event.VenueEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

/**
 * Smoke test to ensure SorModule can be constructed and its lifecycle
 * can be started and stopped without throwing.
 */
class SorModuleSmokeTest {

    @Test
    void sor_starts_and_stops() {
        IdFactory ids = IdFactory.testIds(42L);
        EventQueue<Envelope<OrderEvent>> sorEventBus = new JdkQueueChannel<>();
        EventQueue<Envelope<PubParentIntent>> parentIntentBus = new JdkQueueChannel<>();
        EventQueue<Envelope<PubChildIntent>> childIntentBus = new JdkQueueChannel<>();
        EventQueue<Envelope<VenueEvent>> venueEventBus = new JdkQueueChannel<>();

        VenueRouter venueRouter = mock(VenueRouter.class);

        DualTimeSource dualTimeSource = TestClocks.msTicker();
        EnvelopeSeqGenerator seqGen = mock(EnvelopeSeqGenerator.class);
        EnvelopeMetaFactory metaFactory = new EnvelopeMetaFactory(seqGen, dualTimeSource);

        var sor = new SorModule(sorEventBus, parentIntentBus, childIntentBus, venueEventBus, venueRouter, ids.child(), ids.childClOrd(), metaFactory);

        assertDoesNotThrow(sor::start);
        assertDoesNotThrow(sor::stop);
    }
}
