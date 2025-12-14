package io.tradecraft.common.envelope;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serializable;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonPropertyOrder({
        "payloadType",
        "payload",
        "meta",
        "sealed"
})
public final class Envelope<T> implements Serializable {

    private final T payload;
    private final Meta meta;
    private boolean sealed;

    public static <T> Envelope<T> envelope(T payload) {
        EnvelopeMeta meta = EnvelopeMeta.builder()
                .schemaVersion(1)
                .build();
        return Envelope.of(payload, meta);
    }

    private Envelope(Builder<T> builder) {
        this.payload = builder.payload;
        this.meta = builder.meta;
    }

    public Envelope(T payload, Meta meta) {
        this.payload = payload;
        this.meta = meta;
    }

    public T payload() {
        return payload;
    }

    @JsonProperty("payloadType")
    public String payloadType() {
        return payload.getClass().getSimpleName();
    }

    public Meta meta() {
        return meta;
    }

    public void sealed() {
        sealed = true;
        meta.sealed();
    }
    // ---------- Builder ----------
    public static final class Builder<T> {
        private T payload;
        private Meta meta;

        public Builder<T> payload(T payload) {
            this.payload = payload;
            return this;
        }

        public Builder<T> meta(Meta meta) {
            this.meta = meta;
            return this;
        }

        public Envelope<T> build() {
            if (payload == null)
                throw new IllegalStateException("payload must be set");
            if (meta == null)
                throw new IllegalStateException("meta must be set");
            return new Envelope<>(this);
        }
    }

    // ---------- Static helper ----------
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static <T> Envelope<T> of(T payload, Meta meta) {
        return new Envelope<>(payload, meta);
    }

    public static <T> Envelope<T> of(T payload) {
        return new Envelope<>(payload, EnvelopeMeta.builder().build());
    }

    @Override
    public String toString() {
        return "Envelope{" +
                "payload=" + payload +
                ", meta=" + meta +
                '}';
    }
}