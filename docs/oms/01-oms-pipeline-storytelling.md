# TradeCraft — OMS & Pipeline Story (oms/ + fixqfj/)

> A hands‑on, narrative walkthrough of how the **OMS** behaves from the moment a trader says “New Order” to the moment the world hears an execution, with a spotlight on the **Pipeline** that makes it deterministic, debuggable, and extensible.

---

## TL;DR
- **OMS is a single‑threaded reactor** with a clear **Pipeline**: _ingest_ → _validate_ → _reduce state_ → _emit intents/events_ → _effects_.
- **Commands** change intent; **Events** narrate facts; **Reducers** are the truth keepers; **Effects** talk to the outside world (e.g., FIX, SOR).
- **Identity over process**: ParentId / ChildId / ClOrdId / ExecId anchor concurrency, replay, and idempotency.
- **Deterministic by construction** via: single reactor loop, striped state stores only where safe, `DualTimeSource`, and seeded `IdFactory`.

---

## Where things live

```
oms/
  core/                  # Reducers, FSMs, state stores, intents/events
  pipeline/              # Stage definitions and composition
  effects/               # Side-effects: send-to-FIX, publish-to-SOR, persist
  id/                    # IdFactory, ClOrdId strategy, ExecId
  time/                  # DualTimeSource (monotonic + wall)
  bus/                   # Inbound/Outbound event queues, listeners
  fsm/                   # Parent/Child order state machines (if separated)

fixqfj/
  server/                # OmsFixServerModule: QuickFIX/J acceptor (OMS side)
  session/               # SessionIndex, mappings to sessions/senders
  codecs/                # FIX ↔︎ Domain translators (tags ↔︎ domain fields)
  log/                   # LogFactory wiring and FIX diagnostics
```

> Names may vary in your repo; this map is the conceptual contract used below.

---

## Cast of characters
- **Trader**: speaks FIX in; expects FIX out (ACK/ER).
- **OMS**: the storyteller; holds canonical parent order state.
- **Pipeline**: the path every input walks—no shortcuts.
- **Reducers (pure)**: compute next state from `(state, event)`.
- **Effects (impure)**: emit FIX, publish to SOR, write audit.
- **IdFactory**: for `ParentId`, `ChildId`, `ExecId`, `ClOrdId`.
- **DualTimeSource**: `(monoNanos, wallClock)` per record.
- **SessionIndex**: routes messages to the right FIX session.

---

## The story (Acts I–IV)

### Act I — “A New Order Arrives” (Ingress)
1. **FIX NewOrderSingle** lands in **QuickFIX/J Acceptor** (`fixqfj/server`).
2. A **codec** translates FIX → domain **EvNew** (no mutation yet).
3. The **Pipeline** accepts `EvNew` on the **Inbound Bus** with a precise timestamp (from `DualTimeSource`).
4. Early gatekeepers (syntax/semantic validators, fat‑finger prechecks if configured in OMS) may annotate or reject with a FIX reject.

**Outcome**: a domain event enters OMS with identity seeded (e.g., `ParentId` is minted if needed; `ClOrdId` is mapped/registered).

---

### Act II — “Reduce to Truth” (State & Intents)
1. `EvNew` flows to the **ParentOrder FSM/Reducer**.
2. The **Reducer** computes a new **ParentState** and emits a **ParentRouteIntent** ("this parent wants routing").
3. The **Intent** is tagged with identity (ParentId, optional ClientKey) and causality (source event + ts).

**Why it matters**: Reducers are **pure functions** → replayable, testable, deterministic. Intents are the **bridge** from state to side‑effects.

---

### Act III — “Effects Speak to the World” (Egress)
1. The **Effects** layer consumes `ParentRouteIntent` and **publishes** a corresponding **EvParentRouted** (fact) and a **RouteCommand** to SOR (if configured) or to a venue shortcut in **ImmediateFill** lab setups.
2. In parallel (still ordered in the reactor), OMS emits a **FIX ExecutionReport (ACK)** back to the Trader through `fixqfj` using **SessionIndex** to format and deliver.
3. All external emissions are **idempotent**: dedupe keys are `CommandId` or `(ParentId, tsNanos)` windows.

**Safety rails**: Effects must not mutate order state—only emit or acknowledge. All state lives behind reducers and stores.

---

### Act IV — “Executions Return” (Feedback)
1. When fills come back (via Venue/SOR → OMS), they enter as **EvFill/EvPartial/EvDone**.
2. The **Reducer** updates `cumQty`, `leavesQty`, `avgPx`, and emits **EvParentUpdated** and a translated **FIX ExecutionReport (Trade/Done)**.
3. If a parent requested cancel/replace, the reducer decides whether to emit a **ChildCancelIntent** (guarded by Parent/Child state) and the Effects publish that downstream.

**Result**: the book of record is the OMS **event log + state stores**; FIX is a projection.

---

## Pipelines, precisely

```
[Inbound FIX]
   ↓  (codec FIX→domain)
[Ingress Stage]
   ↓  (validate, enrich ids, stamp time)
[Reducer Stage]
   ↓  (pure: state + event → new state + intents + facts)
[Effects Stage]
   ↓  (impure: FIX out, SOR publish, persistence)
[Outbound Buses]
```

**Why a pipeline?**
- **Observability**: each stage logs `(who, what, when, why)`.
- **Replay**: feed the same event stream → same end state.
- **Testing**: inject stage doubles to assert contracts.

---

## Command vs Event (and where they live)
- **Command**: a *request* to change the world (e.g., _RouteChildCmd_, _CancelParentCmd_). Usually created inside OMS Effects or received from Trader as FIX.
- **Event**: a *fact* that something happened (e.g., _EvNew_, _EvAck_, _EvFill_, _EvCanceled_). Only reducers turn events into state.
- **Intent**: a *decision* the reducer made (e.g., _ParentRouteIntent_, _ChildCancelIntent_). Intents are internal to OMS/SOR boundary.

> Rule of thumb: **Reducers consume events and produce intents; Effects consume intents and produce commands + external messages.**

---

## Identity & Idempotency
- **Lock on identity, not on process**
  - OMS transitions lock on **ParentId**.
  - Downstream SOR locks on **ChildId** stripes.
  - Venues lock on **InstrumentKey** for book determinism.
- **Id keys**
  - Trader: `ClOrdId` (external) ↔︎ OMS: `ParentId` (internal)
  - Child routing: `ChildId` (internal) ↔︎ Venue `VenueOrderId` (external)
  - Executions: `ExecId` per venue execution atom
- **Idempotency windows**: cache last N command/execution ids to drop duplicates safely.

---

## Time and determinism
- **DualTimeSource** carries both **monotonic** (ordering) and **wall** (audit). Every record/event is stamped on ingress.
- Single‑threaded **reactor** = deterministic causality.
- Any parallelism (e.g., persistence, metrics) must be **off the hot path** and observational only.

---

## FIX wiring (fixqfj/)
- **OmsFixServerModule** boots a `SocketAcceptor` with injected factories (`MessageStoreFactory`, `LogFactory`, `MessageFactory`).
- **SessionIndex** maps orders to the correct session (SenderCompID/TargetCompID) for replies.
- **Codecs** translate:
  - **Inbound**: `NewOrderSingle/Cancel/Replace` → `EvNew/EvCancel/EvReplace`
  - **Outbound**: `ACK/Trade/Done/Reject` → `ExecutionReport` with proper `OrdStatus`, `ExecType`, `LeavesQty`, `CumQty`, `AvgPx`, `TransactTime`.

---

## End‑to‑end example (ASCII sequence)

```
Trader        FIX Acceptor        OMS Pipeline                 Effects         SOR/Venue
  |   NewOrderSingle  |                |                          |                |
  |------------------>|  FIX→EvNew     |                          |                |
  |                   |--------------->|  Reduce Parent (EvNew)   |                |
  |                   |                |  -> ParentRouteIntent    |                |
  |                   |                |------------------------->| Publish Route  |
  |                   |                |                          |--------------->|
  |                   |                |------------------------->| ExecReport ACK |
  |<---------------------------------------------------------------------------ACK |
  |                   |                |                          |                |
  |                   |                |       (later)            |                |
  |                   |                |<-------------------------| EvFill         |
  |                   |                | Reduce Parent (EvFill)   |                |
  |                   |                |------------------------->| ExecReport TRADE
  |<-------------------------------------------------------------------- TRADE ER  |
```

---

## Concurrency posture
- **OMS reactor**: single thread for causality.
- **Pipelines**: each stage is synchronous inside the loop.
- **Safe parallelism**: telemetry, audit persistence (append‑only), or outbound IO can be queued off‑thread but must not reenter state directly.

> If needed later: introduce **mailboxes** per external sink (e.g., per‑session FIX sender, per‑venue venueRouter) to bound and observe back‑pressure.

---

## Patterns used (cheat sheet)
| Layer | Pattern(s) | Why |
|---|---|---|
| Pipeline | **Reactor / Event Pipeline** | Deterministic flow, clear stage contracts |
| Reducers | **State Machine + Event Sourcing Lite** | Pure state transitions, easy replay |
| Effects | **Ports & Adapters** | Side‑effects behind interfaces (FIX, SOR) |
| Identity | **Value Objects** (`ParentId`, `ChildId`, `ExecId`) | Stable keys, equality semantics |
| In/Out | **Command Envelope** | Correlation, idempotency windows |
| FIX | **Codec / Translator** | Decouple FIX tags from domain |
| Time | **Dual Clock** | Reproducible order + auditability |

---

## Extending the OMS Pipeline
- **Add a new validator**: place in **Ingress** before reducers.
- **Add risk**: emit `Reject` events in reducer so rejects are facts.
- **Add persistent store**: tap Effects with an append‑only sink (Chronicle/Files/DB) keyed by `(ParentId, seq)`.
- **Add metrics**: measure per‑stage latency and queue depth.
- **Integrate SOR**: consume `ParentRouteIntent` and publish `RouteCommand` to SOR’s mailbox.

---

## Testing & Replay
- **Stage tests**: feed `EvNew` → assert **ParentState** and **ParentRouteIntent**.
- **Golden files**: serialize event streams; re‑ingest → assert equality.
- **FIX round‑trip**: given a domain event, assert the exact FIX ER fields.

---

## Glossary
- **EvNew / EvFill / EvCancel / EvReplace**: domain events from FIX or venues.
- **ParentRouteIntent / ChildCancelIntent**: decisions the reducer made.
- **ExecutionReport (ER)**: FIX projection of OMS facts.
- **SessionIndex**: lookup for where to reply (which FIX session).

---

### Appendix – Story cues you can reuse in docs
- *“Reducers tell the truth; Effects tell the world.”*
- *“Lock on identity, not on process.”*
- *“Intents are bridges, not truths.”*
- *“FIX is a projection; OMS is the book of record.”*

