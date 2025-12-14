package io.tradecraft.oms.event;

import java.util.concurrent.TimeUnit;

public interface EventQueue<T> {
    boolean offer(T item);

    T poll();

    T poll(long timeout, TimeUnit unit) throws InterruptedException;

    int size();
}
