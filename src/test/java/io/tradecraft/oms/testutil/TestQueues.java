package io.tradecraft.oms.testutil;

import io.tradecraft.oms.event.EventQueue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class TestQueues {
    /**
     * Thread-safe queue that also keeps a copy of offered items for assertions.
     */
    public static final class CapturingQueue<T> implements EventQueue<T> {
        public final List<T> items = Collections.synchronizedList(new ArrayList<>());
        private final LinkedBlockingQueue<T> q = new LinkedBlockingQueue<>();

        @Override
        public boolean offer(T item) {
            items.add(item);
            return q.offer(item);
        }

        @Override
        public T poll() {
            return q.poll();
        }

        @Override
        public T poll(long timeout, TimeUnit unit) throws InterruptedException {
            return q.poll(timeout, unit);
        }

        @Override
        public int size() {
            return q.size();
        }
        // default publish(evt) delegates to offer(evt) — no override needed
    }
}

