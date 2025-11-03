# Harbor City by the Sea
`TradeCraft` ``VenueModule`` narrative and design patterns  
Mnemonic: Tag → Tube → Gate → Council → Ledger → Clock → Shell → Lighthouse

---

## Overview

The VenueModule models a simulated exchange inside TradeCraft.  
Each layer represents a role in "Harbor City by the Sea" — a coastal micro-society of clean patterns and single-threaded logic.

Our SOR tags every intent with a CommandId (Command Envelope, CQRS) and sends a VenueCommand that carries that identity.  
DefaultVenue acts as the Gate with an idempotency cache (Map<CommandId, VenueExecution>).  
New commands flow through a Strategy Council (risk → match → fill) operating over a Repository of VenueOrders.  
Outcomes become an immutable VenueExecution (Value Object), stamped by DualTimeSource, cached for replay, and broadcast via VenueListener (Observer).

The venue runs on a single-threaded event loop.  
Safety is achieved by single-writer design, not explicit concurrency guards.

---

## Storyline – Life in Harbor City

| # | Role | Pattern(s) | Description |
|---|------|-------------|--------------|
| 1 | Tag | Command Envelope, CQRS | The SOR ticket booth tags each intent with a unique CommandId. |
| 2 | Tube | Identity via CommandId | VenueCommand carries that identity from origin to venue. |
| 3 | Gate | Idempotency Cache | DefaultVenue checks Map<CommandId, VenueExecution> to replay or process once. |
| 4 | Council | Strategy Pattern | Swappable behaviors – FatFingerRiskStrategy, MatchingEngineStrategy, ImmediateFillStrategy. |
| 5 | Ledger | Repository Pattern | VenueOrderRepository / InMemoryOrderRepository hold live VenueOrders. |
| 6 | Clock | Time Abstraction | DualTimeSource stamps real + simulated time for deterministic replay. |
| 7 | Shell | Value Object Pattern | VenueExecution – immutable, auditable, repeatable output. |
| 8 | Lighthouse | Observer Pattern | VenueListeners emit results to OMS/SOR/audit/metrics asynchronously. |

---

## Architectural Sketch

```
SOR (Tag CommandId)
   │
   ▼  Tube (VenueCommand)
+------------------------------------+
|        DefaultVenue (Gate)         |-- checks idem: Map<CommandId, VenueExecution>
+-------------+--------------+-------+
              |              | miss → process
              | hit          ▼
              |      Council (Strategy) → over
              |          Ledger (Repository)
              |              │
              ▼              ▼
      return cached     Shell (VenueExecution, immutable)
                        │ cache by CommandId
                        └→ Lighthouse (VenueListener, decoupled)

Clock: DualTimeSource stamps events.  
Loop: single-threaded, single-writer event loop (no locks).
```

---

## Key Design Rules

### Idempotency and Replay
- Key: exact CommandId (not payload hash).
- Value: full VenueExecution object.
- Semantics: "same command ⇒ same result."
- Cache only after a terminal decision (ACK / REJECT / FILL / PARTIAL).
- Consider LRU/TTL or persistence if retries arrive much later.

### Strategy Council
- Order: Risk → Match → Fill.
- Purity: each strategy returns a new VenueDecision with no side-effects.
- Termination: stop on terminal decision.
- Composition: (ctx, decision) → decision.

### Single-Threaded Safety
- The venue processes commands in one thread.
- No shared-state races, no locks, deterministic behavior.
- To parallelize later: wrap venue in an actor mailbox or add a real guard.

---

## Minimal Example

```java
public final class DefaultVenue implements Venue {
    private final VenueId venueId;
    private final Map<CommandId, VenueExecution> idem; // Gate logbook
    private final VenueOrderRepository repo;           // Ledger
    private final List<VenueStrategy> council;         // Risk → Match → Fill
    private final List<VenueListener> listeners;       // Lighthouse
    private final DualTimeSource time;                 // Clock

    public DefaultVenue(VenueId id, VenueOrderRepository repo,
                        List<VenueStrategy> council, List<VenueListener> listeners,
                        DualTimeSource time) {
        this.venueId = id;
        this.repo = repo;
        this.council = council;
        this.listeners = listeners;
        this.time = time;
        this.idem = new HashMap<>();
    }

    @Override public VenueId id() { return venueId; }

    @Override
    public void onCommand(VenueCommand cmd) {
        var cached = idem.get(cmd.commandId());
        if (cached != null) { emit(cached); return; }

        VenueContext ctx = VenueContext.of(time.now(), repo);
        VenueDecision decision = VenueDecision.start(cmd);
        for (var s : council) {
            decision = s.apply(ctx, decision);
            if (decision.isTerminal()) break;
        }

        VenueExecution exec = decision.toExecution();
        idem.put(cmd.commandId(), exec);
        emit(exec);
    }

    private void emit(VenueExecution exec) {
        for (var l : listeners) l.onExecution(exec);
    }
}
```

---

## Testing Checklist

- Idempotency: repeat same CommandId → identical VenueExecution.
- Strategy order: invalid orders rejected by risk before match.
- Repository state: verify inserts, cancels, and resting book transitions.
- Clock control: freeze DualTimeSource to assert deterministic times.
- Observer behavior: slow listeners should not block onCommand.

---

## Mnemonic Recap

Tag → Tube → Gate → Council → Ledger → Clock → Shell → Lighthouse  
Harbor City by the Sea:  
Customs tags → Tubes deliver → Gate checks → Council decides → Ledger records → Clock stamps → Shell forms → Lighthouse shines.

---

### Appendix: Why no "Bouncer"

Earlier drafts used a "Bouncer" metaphor.  
In this single-threaded venue, no concurrency guard is required —  
the event loop itself enforces single-writer semantics.  
If concurrency is introduced later, replace with an actor mailbox, striped locks, or critical section guard.
