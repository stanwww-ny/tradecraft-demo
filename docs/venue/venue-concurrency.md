# 🧩 Venue Concurrency Model

This document explains why the **`Venue`** component in `TradeCraft` is intentionally **single-threaded** — and how concurrency is achieved at higher layers.

---

## 1. What a `Venue` Represents

A `Venue` simulates a **single exchange** (e.g., NASDAQ, ARCA, CME).

It owns:
- An **order book** (`Price → FIFO`)
- A **matching engine** that enforces strict sequencing
- **Venue state**: active orders, timestamps, sequence IDs, etc.

➡️ Each `Venue` must behave like a **single-threaded sequencer**, just like a real exchange.

---

## 2. Determinism Requirement

Matching must happen in **strict time order**.

Parallel access to the same order book introduces nondeterministic results:

| Thread | Operation | Effect |
|:-------|:-----------|:-------|
| T1 | Add BUY @ 200 | Book state A |
| T2 | Add SELL @ 200 | Book state B |
| Race | Who matched first? | ❌ Undefined result |

Hence, a **single-threaded loop** ensures deterministic replay:
> Same input → Same output → Reproducible simulation

---

## 3. Where Concurrency *Is* Allowed

| Layer | Threading Model | Purpose |
|:------|:----------------|:--------|
| **Venue** | Single-threaded | Deterministic sequencing |
| **VenueRouter** | Multi-threaded | Dispatches to individual venues |
| **SOR (Smart Order Router)** | Multi-threaded | Routes parent → child orders concurrently |
| **OMS** | Multi-threaded | Manages multiple parent orders |

✅ Multiple venues can run **in parallel**, each on its own thread.

```
[SOR Router] ──┬─> [Venue-NYSE Thread]
               ├─> [Venue-ARCA Thread]
               └─> [Venue-NASDAQ Thread]
