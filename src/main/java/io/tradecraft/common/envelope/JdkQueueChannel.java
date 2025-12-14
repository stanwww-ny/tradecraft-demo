package io.tradecraft.common.envelope;

import io.tradecraft.oms.event.EventQueue;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public final class JdkQueueChannel<P> implements EventQueue<Envelope<P>> {

    ConcurrentLinkedQueue<Envelope<P>> queue = new ConcurrentLinkedQueue<>();

    @Override public boolean offer(Envelope<P> env) { return queue.offer(env); }
    @Override public Envelope<P> poll() { return queue.poll(); }
    @Override public Envelope<P> poll(long t, TimeUnit u) throws InterruptedException {
        throw new UnsupportedOperationException("Timed poll not supported by non-blocking queue");
    }
    @Override public int size() { return queue.size(); }
}