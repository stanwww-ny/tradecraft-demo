package io.tradecraft.oms.core;

import io.tradecraft.common.spi.oms.exec.PubExecReport;
import io.tradecraft.common.spi.oms.intent.PubParentIntent;
import io.tradecraft.oms.core.parentfx.ParentFx;
import io.tradecraft.oms.event.EvNew;
import io.tradecraft.oms.event.OrderEvent;

import java.util.Collections;
import java.util.List;

public record Effects(
        OrderState newState,
        List<PubParentIntent> intents,
        List<OrderEvent> followUps,
        List<PubExecReport> execReports,
        List<ParentFx> parentFxes
) {

    public static Builder withState(OrderState s) {
        return new Builder(s);
    }

    public static Effects none() {
        return new Effects(null, List.of(), List.of(), List.of(), List.of());
    }

    public static Effects initial(EvNew evNew, OrderState newState) {
        return new Effects(
                newState,
                Collections.emptyList(),   // no ERs
                Collections.emptyList(),   // no intents
                Collections.emptyList(),   // no follow-ups
                Collections.emptyList()    // no FX
        );
    }
    public static final class Builder {
        private final OrderState newState;
        private List<PubParentIntent> intents = List.of();
        private List<OrderEvent> evts = List.of();
        private List<PubExecReport> execReports = List.of();
        private List<ParentFx> parentFxes = List.of();

        public Builder(OrderState newState) {
            this.newState = newState;
        }

        public Builder intents(PubParentIntent... intents) {
            this.intents = List.of(intents);
            return this;
        }

        public Builder intents(List<PubParentIntent> intents) {
            this.intents = intents;
            return this;
        }

        public Builder followUps(OrderEvent... events) {
            this.evts = List.of(events);
            return this;
        }

        public Builder ers(List<PubExecReport> reports) {
            this.execReports = reports;
            return this;
        }

        public Builder er(PubExecReport... reports) {
            this.execReports = List.of(reports);
            return this;
        }

        public Builder parentFxes(List<ParentFx> parentFxes) {
            this.parentFxes = parentFxes;
            return this;
        }

        public Effects build() {
            return new Effects(newState, intents, evts, execReports, parentFxes);
        }
    }
}
