package io.tradecraft.venue.registry;

import io.tradecraft.common.domain.instrument.InstrumentKey;
import io.tradecraft.common.domain.market.DomainAccountType;
import io.tradecraft.common.domain.market.DomainOrdType;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.market.DomainTif;
import io.tradecraft.common.domain.time.DualTimeSource;
import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.envelope.EnvelopeMetaFactory;
import io.tradecraft.common.envelope.Meta;
import io.tradecraft.common.id.ChildClOrdId;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.ParentId;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.testing.TestClocks;
import io.tradecraft.venue.api.VenueExecution;
import io.tradecraft.venue.cmd.CancelChildCmd;
import io.tradecraft.venue.cmd.NewChildCmd;
import io.tradecraft.venue.cmd.ReplaceChildCmd;
import io.tradecraft.venue.cmd.VenueCommand;
import io.tradecraft.venue.event.VenueAck;
import io.tradecraft.venue.event.VenueCancelDone;
import io.tradecraft.venue.event.VenueEvent;
import io.tradecraft.venue.event.VenueFill;
import io.tradecraft.venue.event.VenueReject;
import io.tradecraft.venue.listener.VenueListener;
import io.tradecraft.venue.strategy.VenueStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultVenueTest {

    private final DualTimeSource dualTimeSource = TestClocks.msTicker();
    @Mock
    VenueListener listener;
    @Mock
    VenueStrategy s1;
    @Mock
    VenueStrategy s2;
    @Mock
    EnvelopeMetaFactory metaFactory;
    @Mock
    Meta meta;

    private NewChildCmd sampleNew() {
        return NewChildCmd.builder()
                .parentId(ParentId.of("P1"))
                .childId(ChildId.of("CH-1"))
                .childClOrdId(ChildClOrdId.of("CCl-1"))
                .accountId("ACC-1")
                .domainAccountType(DomainAccountType.CUSTOMER)
                .instrumentKey(InstrumentKey.ofSymbol("ABC"))
                .side(DomainSide.BUY)
                .qty(100L)
                .ordType(DomainOrdType.MARKET)
                .tif(DomainTif.DAY)
                .venueId(new VenueId("SIM"))
                .tsNanos(dualTimeSource.nowNanos())
                .build();
    }

    private CancelChildCmd sampleCancel() {
        // adjust if your CancelChildCmd has a builder/ctor signature
        return new CancelChildCmd.Builder().childId(ChildId.of("CH-1")).tsNanos(dualTimeSource.nowNanos()).build();
    }

    private ReplaceChildCmd sampleReplace() {
        // adjust to match your ReplaceChildCmd ctor/builder (qty & price changes illustrative)
        return new ReplaceChildCmd.Builder().childId(ChildId.of("CH-1"))
                .newQty(150L).newLimitPxMicros(101_50L).tsNanos(dualTimeSource.nowNanos()).build();
    }

    @Test
    void new_cmd_chains_whenFirstNoop_thenEmitsMerged_ackThenFill() {
        var venueId = new VenueId("SIM");
        var cmd = sampleNew();

        when(s1.appliesTo(cmd)).thenReturn(true);
        when(s1.decide(cmd)).thenReturn(VenueExecution.noop());

        var ack = mock(VenueAck.class);
        Envelope<VenueEvent> envelope1 = Envelope.of(ack, meta);
        var fill = mock(VenueFill.class);
        Envelope<VenueEvent>  envelope2 = Envelope.of(ack, meta);
        when(s2.appliesTo(cmd)).thenReturn(true);
        when(s2.decide(cmd)).thenReturn(VenueExecution.of(List.of(ack), List.of(fill), null, null));

        var venue = new DefaultVenue(venueId, List.of(s1, s2), listener, metaFactory);
        Envelope<VenueCommand> envelope = Envelope.of(cmd, meta);
        venue.onCommand(envelope);

        InOrder inOrder = inOrder(s1, s2, listener);
        inOrder.verify(s1).appliesTo(cmd);
        inOrder.verify(s1).decide(cmd);
        inOrder.verify(s2).appliesTo(cmd);
        inOrder.verify(s2).decide(cmd);
        inOrder.verify(listener).onEvent(argThat(env -> env.payload() == ack));
        inOrder.verify(listener).onEvent(argThat(env -> env.payload() == fill));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void new_cmd_stopsChain_whenFirstRejects() {
        var venueId = new VenueId("SIM");
        var cmd = sampleNew();

        var reject = mock(VenueReject.class);
        when(s1.appliesTo(cmd)).thenReturn(true);
        when(s1.decide(cmd)).thenReturn(VenueExecution.reject(reject));

        var venue = new DefaultVenue(venueId, List.of(s1, s2), listener, metaFactory);
        Envelope<VenueCommand> envelope = Envelope.of(cmd, meta);
        venue.onCommand(envelope);

        // Verify: s1 used; s2 untouched; listener got reject
        InOrder io = inOrder(s1, listener);
        io.verify(s1).appliesTo(cmd);
        io.verify(s1).decide(cmd);
        verifyNoInteractions(s2);                 // nothing on s2
        io.verify(listener).onEvent(argThat(env -> env.payload() == reject));
        io.verifyNoMoreInteractions();
    }

    @Test
    void cancel_cmd_terminalCancel_shortCircuits() {
        var venueId = new VenueId("SIM");
        var cmd = sampleCancel();

        var cancel = mock(VenueCancelDone.class);
        when(s1.appliesTo(cmd)).thenReturn(true);
        when(s1.decide(cmd)).thenReturn(VenueExecution.cancel(cancel));

        var venue = new DefaultVenue(venueId, List.of(s1, s2), listener, metaFactory);
        Envelope<VenueCommand> envelope = Envelope.of(cmd, meta);
        venue.onCommand(envelope);

        InOrder io = inOrder(s1, s2, listener);
        io.verify(s1).appliesTo(cmd);
        io.verify(s1).decide(cmd);
        io.verify(s2, never()).decide(any());
        io.verify(listener).onEvent(argThat(env -> env.payload() == cancel));
        io.verifyNoMoreInteractions();
    }

    @Test
    void replace_cmd_skipsNonMatching_thenProcessesMatching() {
        var venueId = new VenueId("SIM");
        var cmd = sampleReplace();

        when(s1.appliesTo(cmd)).thenReturn(false);

        var ack = mock(VenueAck.class);
        when(s2.appliesTo(cmd)).thenReturn(true);
        when(s2.decide(cmd)).thenReturn(VenueExecution.events(List.of(ack), List.of()));

        var venue = new DefaultVenue(venueId, List.of(s1, s2), listener, metaFactory);
        Envelope<VenueCommand> envelope = Envelope.of(cmd, meta);
        venue.onCommand(envelope);

        InOrder io = inOrder(s1, s2, listener);
        io.verify(s1).appliesTo(cmd);
        io.verify(s2).appliesTo(cmd);
        io.verify(s2).decide(cmd);
        io.verify(listener).onEvent(argThat(env -> env.payload() == ack));
        io.verifyNoMoreInteractions();
    }

    @Test
    void throws_if_no_strategy_matches_for_new() {
        var venueId = new VenueId("SIM");
        var cmd = sampleNew();
        Envelope<VenueCommand> envelope = Envelope.of(cmd, meta);
        when(s1.appliesTo(cmd)).thenReturn(false);
        when(s2.appliesTo(cmd)).thenReturn(false);

        var venue = new DefaultVenue(venueId, List.of(s1, s2), listener, metaFactory);

        assertThrows(IllegalStateException.class, () -> venue.onCommand(envelope));
        verifyNoInteractions(listener);
    }
}
