# OMS Ingress Architecture — Mapper + Bridge Pattern

## Overview

The OMS ingress is designed as a **two-step Anti-Corruption Layer (ACL)** built around the FIX acceptor.

```
OmsFixInbound.onMessage(session, fixMsg)
   ├─ FixInboundMapper.bind(fixMsg)          // FIX → FixEv*
   └─ FixInboundBridge.offer(FixEv*)         // FixEv* → EvBound* → inboundBus
```

This structure lets the OMS core stay **protocol-agnostic** while still supporting
multiple inbound protocols in the future (FIX, REST, WebSocket).

---

## 1️⃣ Components and Responsibilities

### **OmsFixServerModule**
*appliesTo:* lifecycle wiring  
*decides:* which inbound bridges are active, factory configuration, queue sizes

- Constructs and wires the FIX inbound stack (`OmsFixInbound`, `FixInboundMapper`, `FixInboundBridge`, `SessionIndex`, `inboundBus`).
- Starts and stops the acceptor.
- May later include additional bridges (e.g., `RestInboundBridge`).

---

### **OmsFixServer**
*appliesTo:* holder of references  
*decides:* nothing

- Contains `SessionSettings`, `MessageFactory`, `LogFactory`, `MessageStoreFactory`.
- Simplifies module construction; does not process messages.

---

### **OmsFixInbound (ACCEPTOR)**
*appliesTo:* QuickFIX/J `Application` (I/O edge)  
*decides:* when to bind and bridge; owns backpressure/error policy

- Runs the **QuickFIX/J `SocketAcceptor`**.
- Handles session events (`onLogon`, `onLogout`) and updates `SessionIndex`.
- On inbound FIX:
    1. Calls `FixInboundMapper.bind(session, fixMsg)` → produces **FixEv***.
    2. Calls `FixInboundBridge.offer(fixEv)` → produces **EvBound***.
    3. Enqueues `EvBound*` onto the **`inboundBus`** (bounded CQRS handoff).

---

### **FixInboundMapper**
*appliesTo:* FIX → protocol-safe events  
*decides:* tag normalization and syntactic validation

- Parses and normalizes FIX messages into typed **FixEv*** objects:
    - `35=D` → `FixEvNew`
    - `35=F` → `FixEvCancel`
    - `35=G` → `FixEvReplace`
- Performs basic tag checks (e.g., presence of `11=ClOrdID`, correct side/ordType values).
- Does *not* enforce domain rules.

---

### **FixInboundBridge**
*appliesTo:* protocol-safe → domain-safe conversion  
*decides:* dialect strategy, validation, id/time stamping, idempotency, queue policy

- Converts **FixEv*** into **EvBound*** objects (domain events).
- Applies per-session **Dialect Strategies** (price scaling, symbol mapping, TIF translation, etc.).
- Enforces **policy guards**: qty > 0, LIMIT requires price, cancel requires `origClOrdId`, etc.
- Stamps `SessionKey`, and `DualTimeSource` timestamps.
- Optional **idempotency cache** by `(session, clOrdId)`.
- Enqueues `evBound's` to `inboundBus` or rejects immediately via `SessionIndex`.

---

### **(Future) RestInboundBridge / WsInboundBridge**
*appliesTo:* alternative ingress edges  
*decides:* mapping REST/WS payloads to `EvBound` and sharing the same enqueue logic

All bridges implement a common interface:
```java
public interface InboundBridge {
    boolean offer(Object inboundEvent);
}
```

---

### **SessionIndex**
*appliesTo:* session registry  
*decides:* outbound routing for rejects, ACKs, ExecutionReports

---

### **inboundBus (EventQueue<OrderEvent>)**
*appliesTo:* CQRS write handoff  
*decides:* pacing and ordering for the single-threaded pipeline/reactor

---

### **Pipeline (in oms/)**
*appliesTo:* domain processing  
*decides:* state transitions and strategy execution

- Consumes `EvBound*` events.
- Runs reducers and FSMs for Parent/Child orders.
- Emits `OrderEvent*` objects (ACK/FILL/REJECT) to the Outbound ACL.

---

## 2️⃣ Data Flow

```
Client FIX
  └─► OmsFixInbound.onMessage(session, fixMsg)
        ├─ fixEv = FixInboundMapper.bind(session, fixMsg)      // FIX → FixEv*
        └─ FixInboundBridge.offer(fixEv)                       // FixEv* → EvBound*
              ├─ apply dialect strategy & validate
              ├─ stamp IDs & DualTimeSource time
              ├─ idempotency check (optional)
              ├─ if invalid → reject via SessionIndex
              └─ inboundBus.offer(evBound, timeout)
                    └─ Pipeline → Reducers/FSM → OrderEvents
                          └─ Outbound ACL + MessageFactory → SessionIndex.route()
```

---

## 3️⃣ Patterns Summary

| Stage | Component                     | Patterns | Purpose |
|---|-------------------------------|---|---|
| Bootstrap | OmsFixServerModule            | Hexagonal Ports, DI | Keep OMS core protocol-free |
| FIX I/O | OmsFixInbound                 | Gateway, Command Envelope | Deterministic, single entrypoint |
| Bind | FixInboundMapper              | Mapper, ACL (Step 1) | Parse & normalize FIX safely |
| Bridge | FixInboundBridge              | Bridge, Strategy, ACL (Step 2), Policy Guards, Idempotency, Deterministic Time | Domain validation & queue handoff |
| Handoff | inboundBus                    | Bounded Mailbox, CQRS (write) | Backpressure & causal ordering |
| Domain | Pipeline/FSM                  | Reactor, Reducer/FSM, Observer | Deterministic business logic |
| Egress | Outbound ACL + MessageFactory | ACL, Factory | Counterparty-clean ER/Ack building |

---

## 4️⃣ Why Two-Step ACL?

| Layer | Responsibility | Benefit |
|---|---|---|
| **Mapper** | Syntax binding (FIX → FixEv*) | Shields parser complexity; easy unit tests |
| **Bridge** | Semantic binding (FixEv* → EvBound*) | Enforces domain rules; swappable for other protocols |
| **Core** | Pure domain (EvBound* onward) | Protocol-agnostic OMS and deterministic replay |

---

## 5️⃣ Extensibility Example

### Adding a REST Edge

```java
public final class RestInboundBridge implements InboundBridge {
    private final EventQueue<OrderEvent> inboundBus;
    private final DualTimeSource time;

    @Override
    public boolean offer(Object dto) {
        RestOrderDto rest = (RestOrderDto) dto;
        EvBound ev = mapRestDtoToEvBound(rest, time.now());
        return inboundBus.offer(ev, 50, TimeUnit.MILLISECONDS);
    }
}
```

Register both bridges in the module:
```java
InboundBridge fixBridge = new FixInboundBridge(...);
InboundBridge restBridge = new RestInboundBridge(...);
InboundRouter venueRouter = new InboundRouter(List.of(fixBridge, restBridge));
```

Now FIX and REST share the same `inboundBus` and deterministic OMS pipeline.

---

## 6️⃣ appliesTo / decides (for diagrams)

| Component | appliesTo | decides |
|---|---|---|
| OmsFixServerModule | lifecycle, DI | which bridges/factories exist |
| OmsFixInbound | FIX acceptor | when to bind & bridge |
| FixInboundMapper | parsing & normalization | field mappings |
| FixInboundBridge | domain ingress | validation, id/time, enqueue policy |
| inboundBus | flow control | pacing, ordering |
| OMS Reactor | state machine | business logic outcomes |
| SessionIndex | routing | where outbound messages go |

---

## 7️⃣ Folder Responsibilities

### `fixqfj/`
- `OmsFixInbound` — FIX Acceptor (`Application`)
- `FixInboundMapper` — FIX → FixEv*
- `FixInboundBridge` — FixEv* → EvBound*
- `SessionIndex` — session registry
- `OmsFixServerModule` / `OmsFixServer` — lifecycle holders

### `oms/`
- `EventQueue<OrderEvent>` — bounded CQRS handoff
- `DefaultParentOrderFsm`, `ChildReducer` — domain FSM/reducers
- `DualTimeSource`, `IdFactory` — deterministic replay
- `VenueListener`, `Outbound ACL` — egress mapping

---

## 8️⃣ Testing Checklist

- **Mapper tests** — FIX samples → FixEv* (D/F/G with edge cases)
- **Bridge tests** — FixEv* matrices → EvBound* or reject (policy guards)
- **Idempotency tests** — duplicate ClOrdID within session → reject/suppress
- **Queue backpressure tests** — simulate full inboundBus → verify metrics & behavior
- **Integration tests** — FIX D/F/G → OrderEvents → FIX ExecutionReports
- **Replay tests** — feed EvBound logs → assert deterministic outcomes

---

**Summary**

> The OMS ingress separates **protocol syntax** from **domain semantics**.  
> `FixInboundMapper` performs the *binding*;  
> `FixInboundBridge` performs the *bridging*;  
> and `OmsFixInbound` acts as the *gateway*.  
> This architecture isolates external protocols, enables deterministic replay, and
> makes it trivial to introduce new inbound edges (REST, WebSocket, gRPC) using the same core pipeline.
