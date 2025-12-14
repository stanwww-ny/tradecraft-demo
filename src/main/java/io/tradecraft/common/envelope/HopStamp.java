package io.tradecraft.common.envelope;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.io.Serializable;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public final class HopStamp implements Serializable {

    private final Stage stage;
    private final long stageNanos;

    private HopStamp(Builder builder) {
        this.stage = builder.stage;
        this.stageNanos = builder.stageNanos;
    }

    public Stage stage()      { return stage; }
    public long stageNanos()     { return stageNanos; }

    // ---------- Builder ----------
    public static final class Builder {
        private Stage stage;
        private long stageNanos;

        public Builder stage(Stage stage) {
            this.stage = stage;
            return this;
        }

        public Builder stageNanos(long stageNanos) {
            this.stageNanos = stageNanos;
            return this;
        }

        public HopStamp build() {
            if (stage == null) {
                throw new IllegalStateException("stage must be set");
            }
            // inNanos/outNanos can be 0 if you only stamp one side
            return new HopStamp(this);
        }
    }

    // ---------- Static helper ----------
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "HopStamp{" +
                "stage=" + stage +
                ", stageNanos=" + stageNanos +
                '}';
    }
}
