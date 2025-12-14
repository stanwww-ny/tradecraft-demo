package io.tradecraft.common.id.generator;

public interface IdGenerator<T> {
    T next();
}
