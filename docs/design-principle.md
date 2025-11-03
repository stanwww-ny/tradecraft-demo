# 🧠 Design Principle in TradeCraft

## Rule: Lock on Identity, Not on Process

TradeCraft enforces **deterministic concurrency boundaries** through identity-based locking.  
Each subsystem synchronizes access based on **domain identity**, not on internal process or thread affinity.

---

### 🔒 Concurrency Boundaries by Identity

| Subsystem | Lock Identity | Purpose |
|------------|----------------|----------|
| **OMS** | `ParentId` | Ensures deterministic FSM transitions per parent order. |
| **SOR** | `ChildId` / `ParentId` stripe | Enables safe concurrent state updates across stripes, maintaining causal ordering. |
| **Venue** | `InstrumentKey` | Protects order book integrity for each instrument while allowing multi-instrument parallelism. |

---

```
OMS (lock: ParentId)
  └─ Parent FSMs
      │  ParentIntent/Events
      ▼
SOR (striped lock: ChildId / ParentId)
  ├─ Single-threaded reactor (deterministic causality)
  └─ ChildStateStore [striped]
      │  Commands
      ▼
VenueRouter (Command-Queue boundary)
  ├─ dispatch → Venue A mailbox ─→ VenueListener → Strategies → MatchingEngine [lock: InstrumentKey]
  └─ dispatch → Venue B mailbox ─→ VenueListener → Strategies → MatchingEngine [lock: InstrumentKey]

Feedback loop: MatchingEngine emits VenueEvents → back to SOR.
```
---

### 🧩 Principle

> **Lock by who it belongs to, not by where it happens.**

This approach establishes a consistent concurrency boundary **by domain identity**, not by subsystem.  
It enables **parallel determinism**, **clear causality**, and **predictable replay** across OMS → SOR → Venue.

---

### 🧱 Summary

- Deterministic causality achieved via single-threaded or striped contexts.
- Each lock domain maps to a unique aggregate identity.
- Subsystems communicate asynchronously via event buses or command queues.
- Parallel execution is safe across different identities but serialized within one.

---

📘 *TradeCraft Core Principle — deterministic concurrency through identity-based locking.*
