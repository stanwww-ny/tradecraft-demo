package io.tradecraft.common.utils;
import io.tradecraft.oms.event.EventQueue;

public final class QueueRoundRobinPoller<T> {

    private final EventQueue<T> q1;   // inbound FIX → OMS
    private final EventQueue<T> q2;   // SOR/MQ → OMS
    private int rr = 0;               // 0 = q1 first, 1 = q2 first

    public QueueRoundRobinPoller(EventQueue<T> q1, EventQueue<T> q2) {
        this.q1 = q1;
        this.q2 = q2;
    }

    /** Non-blocking fair poll */
    public T poll() {
        EventQueue<T> first  = (rr == 0 ? q1 : q2);
        EventQueue<T> second = (rr == 0 ? q2 : q1);

        if (first != null) {
            T e = first.poll();
            if (e != null) {
                rr ^= 1; // rotate next time
                return e;
            }
        }
        if (second != null) {
            T e = second.poll();
            if (e != null) {
                rr ^= 1;
                return e;
            }
        }

        // No event found, but rotate anyway to avoid bias
        rr ^= 1;
        return null;
    }
}

