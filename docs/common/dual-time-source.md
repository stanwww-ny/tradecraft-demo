
# DualTimeSource — Two Clocks, One Truth
**Package:** `io.tradecraft.common.domain.time`

A precise, composable time facility for TradeCraft that carries **two independent clocks**:

- **Monotonic nanos** (`nowNanos()`): sequencing, latency, and duration math. Think `System.nanoTime()` semantics.
- **Wall‑clock millis** (`wallClockMillis()` / `wallClockInstant()`): human‑friendly UTC timestamps for FIX fields, logs, metrics, and compliance.

> Never use wall‑clock for ordering. Never mix clocks in the same calculation.

---

## Why a “Dual” Time Source?
Matching engines, SOR, and OMS logic require **deterministic ordering** and **accurate latency** (monotonic). But FIX bridges, audit trails, and dashboards require **human‑readable time** (wall‑clock). `DualTimeSource` encapsulates both—explicitly separating concerns so each module asks the *right* clock the *right* question.

---

## API Overview

```java
interface DualTimeSource {

    // Factory helpers
    static DualTimeSource system();
    static TestDualTimeSource test(long startNanos, long stepNanos, long startMillis);
    static DualTimeSource compose(LongSupplier monotonicNanos, LongSupplier wallClockMillis);

    // Core
    long nowNanos();            // monotonic nanos for ordering and durations
    long wallClockMillis();     // UTC epoch millis for FIX/logs/compliance

    // Convenience
    default Instant wallClockInstant();
    default long wallClockNanos();
    default Duration sinceNanos(long previousNowNanos);
    default LongSupplier monotonicSupplier();
    default LongSupplier wallClockMillisSupplier();
}
```
**Concrete implementations:**
- `SystemDualTimeSource` — prod singleton: monotonic = `System.nanoTime()`, wall = `System.currentTimeMillis()`
- `TestDualTimeSource` — deterministic test/replay: step‑based nanos counter + independently controlled wall‑clock
- `ComposedDualTimeSource` — adapter from two existing suppliers

**Related simple sources:**
- `TimeSource` (nanos‑only) + implementations: `SystemTimeSource`, `FixedTimeSource`, `TestTimeSource`, `SteppingTimeSource`, `ReplayTimeSource`
- `TradingCalendar.endOfDay(tsNanos)` (MVP hard‑coded to `America/New_York 16:00`)

---

## Story‑telling: “Two Suns” in TradeCraft

Picture TradeCraft as a city with **two suns**:

1. **The White Sun (Monotonic Nanos)** — it never sets, only advances. Workers (reducers, FSMs, matchers) use it to line up at the factory: **first in time, first to process**. It judges latency fairly and never jumps backward.
2. **The Yellow Sun (Wall‑Clock)** — what citizens read on clocks and in newspapers. It stamps every receipt and filing (FIX `52=SendingTime`, audit rows, dashboard charts). But it can “jump” (NTP corrections, DST, leap seconds)—so **don’t use it to break ties on the factory floor**.

`DualTimeSource` is the observatory that shows both skies at once. Modules choose the sun they need.

---

## Example Output

```
Timestamp (nanos):  253440173658200
Timestamp (millis): 253440173
```

- **nanos** → super precise, monotonic counter (use for sequencing, latency).
- **millis** → human‑friendly, derived from nanos (use for logs/metrics).
  - In `DualTimeSource`, wall‑clock is independent (not literally derived from nanos), but you may *display* both together for diagnostics.

---

## Usage by Module (Pragmatic Map)

> Short answer: **Inject a shared `DualTimeSource` into most core modules.** Use nanos for durations/ordering; use millis/`Instant` only where the business or external interfaces require calendar time.

| Module / Layer | Inject `DualTimeSource`? | Use **nanos** for… (monotonic) | Use **millis / wall‑clock** for… |
|---|---|---|---|
| App / AppConfig / Bootstrap | Yes (root) | n/a (pass down) | Optional startup banner timestamp |
| **PipelineModule** (single writer) | Yes | recvNanos, enqueueNanos, processStart/End; per‑stage & e2e latency; backoff | Batch wall‑time windows (rare); startup banner |
| **Event Buses / Queues** (`inboundBus`, `sorEvtBus`, `erBus`, `planInBus`) | Yes | enqueue/dequeue stamps; drop/overflow timing | Emission time in logs/metrics exporters |
| **FIX Bridges** (Inbound/Outbound) | Yes | parse/ingress vs processing durations; heartbeat timeouts | FIX fields that require wall‑clock (e.g., `52=SendingTime`); audit interoperable with external systems |
| **OMS core** | Yes | SLA timers (ack/fill/cancel); FSM time deltas | GTD checks; compliance/audit |
| **SOR** (Router + strategies) | Yes | per‑child time budgets; throttles; retry backoffs | Cutovers tied to exchange local time (if modeled) |
| **Venue / Matching Engine** | Yes | match‑cycle timers; book queue time; IOC/FOK enforcement | Market session open/close by exchange calendars |
| **Risk** (FatFinger/Throttle/Credit) | Yes | rolling windows; cool‑offs | Daily resets at midnight (if modeled) |
| **NBBO provider / Market data** | Yes | snapshot `tsNanos` for staleness | human‑readable dashboard timestamps |
| **Replay / Capture / Validate** | Yes (deterministic impl) | drive timing and validate latencies | report run start/stop times |
| **ID/Seed generators** | Optional | prefer RNG; if time‑based, use nanos for monotonicity | Avoid date‑partitioned IDs for MVP |
| **Metrics** (Micrometer) | Yes | high‑res timers, histograms | exporter timestamps (usually wall‑clock) |
| **Logging** | No | (use logger’s clock) | logger’s own timestamps |

**Injection rules of thumb**  
- **Yes, inject** into nearly every core service touching events, market data, risk, or matching.  
- Keep it the **last constructor parameter** (cross‑cutting concern) for consistency.  
- **Default to nanos**; convert to millis/`Instant` only **at the edge** (FIX, human logs, GTD calendars).

---

## Patterns Used

- **Clock Abstraction (Interface Segregation)** — `DualTimeSource` cleanly exposes *two* clocks with a small, explicit API.
- **Strategy / Policy** — selects the *policy of timekeeping* at runtime: `SystemDualTimeSource` (prod), `TestDualTimeSource` (deterministic), `ComposedDualTimeSource` (adapting legacy suppliers).
- **Deterministic Test Double** — `TestDualTimeSource` uses step‑based `AtomicLong` counters for **repeatable** timing in unit/integration tests and replay.
- **Adapter** — `compose(LongSupplier, LongSupplier)` adapts existing clocks without refactors.
- **Edge/Hexagonal Boundary** — conversion to wall‑clock (FIX/logs) is **at the edges**; core logic remains monotonic.
- **Telemetry‑friendly** — suppliers (`monotonicSupplier`, `wallClockMillisSupplier`) plug into metrics systems (histograms, CQ tailers) without allocations.

---

## Code Snippets

### Production
```java
final DualTimeSource ts = DualTimeSource.system();

long t0 = ts.nowNanos();
// ... work ...
long nanos = ts.nowNanos();
Duration stage = ts.sinceNanos(t0);

long sendingTime = ts.wallClockMillis();   // FIX 52
Instant when = ts.wallClockInstant();      // audit / logs
```

### Deterministic Tests / Replay
```java
DualTimeSource.TestDualTimeSource ts = DualTimeSource.test(
    /*startNanos*/ 1_000_000L,
    /*stepNanos*/  500L,
    /*startMillis*/ 1_700_000_000_000L
);

long a = ts.nowNanos(); // 1_000_000
long b = ts.nowNanos(); // 1_000_500
ts.advanceWallClockMillis(250); // bump wall‑clock by 250ms
long wall = ts.wallClockMillis();
```

### Composing From Existing Clocks
```java
DualTimeSource ts = DualTimeSource.compose(
    System::nanoTime,
    System::currentTimeMillis
);
```

---

## Interop with `TimeSource` (nanos‑only)

Some legacy utilities take a nanos‑only `TimeSource`:

```java
public interface TimeSource {
    static TimeSource system() { return System::nanoTime; }
    long nowNanos();
    default long nowMillis() { return TimeUnit.NANOSECONDS.toMillis(nowNanos()); }
}
```

You can bridge with a simple adapter when needed:

```java
TimeSource nanoOnly = ts::nowNanos;  // ts is a DualTimeSource
```

---

## TradingCalendar (MVP helper)

```java
Instant eod = TradingCalendar.endOfDay(ts.nowNanos());
// Currently hard‑coded to 16:00 America/New_York (MVP simplification)
```

---

## Common Pitfalls & Guardrails

- **Do not** sort by wall‑clock; use **monotonic nanos** for priority queues and book ordering.
- **Do not** subtract wall‑clock millis to compute durations; use `sinceNanos(...)`.
- **Do** stamp events with both when you cross a boundary (e.g., inbound FIX → EvBound) for traceability.
- **Do** treat wall‑clock as an *edge concern* (logs, FIX, compliance, calendars).

---

## Drop‑in Refactor Checklist

1. Add a `DualTimeSource` field to each core module (place it **last** in constructors).
2. Replace `System.nanoTime()` calls with `ts.nowNanos()`.
3. Replace `System.currentTimeMillis()` calls with `ts.wallClockMillis()` where external contracts require wall‑clock.
4. For metrics, prefer `ts.monotonicSupplier()` for high‑res timers.
5. In tests, use `DualTimeSource.test(...)` for deterministic timing.

---

## Appendix — Minimal Demo

```java
DualTimeSource ts = DualTimeSource.system();
System.out.println("Timestamp (nanos):  " + ts.nowNanos());
System.out.println("Timestamp (millis): " + ts.wallClockMillis());
```
_Expected (shape):_
```
Timestamp (nanos):  253440173658200
Timestamp (millis): 253440173
```
