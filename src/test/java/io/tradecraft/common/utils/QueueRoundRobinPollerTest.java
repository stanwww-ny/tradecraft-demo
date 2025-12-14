package io.tradecraft.common.utils;

import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.envelope.JdkQueueChannel;
import io.tradecraft.oms.event.EventQueue;
import io.tradecraft.oms.event.OrderEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class QueueRoundRobinPollerTest {
    EventQueue<Envelope<OrderEvent>> q1;
    EventQueue<Envelope<OrderEvent>> q2;
    Envelope<OrderEvent> a1, a2, b1, b2;
    QueueRoundRobinPoller<Envelope<OrderEvent>> poller;

    @BeforeEach
    void setUp() {
        q1 = new JdkQueueChannel<>();
        q2 = new JdkQueueChannel<>();
        a1 = mock(Envelope.class); a2 = mock(Envelope.class);
        b1 = mock(Envelope.class); b2 = mock(Envelope.class);
        poller = new QueueRoundRobinPoller<>(q1, q2);
    }

    @Test
    void testNonBlockingRoundRobin() {
        q1.offer(a1); q1.offer(a2);
        q2.offer(b1); q2.offer(b2);
        assertEquals(a1, poller.poll()); // q1 first
        assertEquals(b1, poller.poll()); // q2 next
        assertEquals(a2, poller.poll()); // q1 again
        assertEquals(b2, poller.poll()); // q2 again
    }

    @Test
    void testEmptyQueuesReturnNull() {
        assertNull(poller.poll()); // q1 first
        assertNull(poller.poll()); // q2 next
        assertNull(poller.poll()); // q1 again
        assertNull(poller.poll()); // q2 again
    }

}

