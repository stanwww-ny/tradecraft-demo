# TradeCraft – OMS + SOR + Venue Simulator + TradeClient Demo

> **Version:** v1.0 (Public Demo Snapshot)

A deterministic, event-driven trading platform prototype demonstrating
**OMS → SOR → Venue** execution flow, built in Java 21 with QuickFIX/J.

This repository is intended for **architectural discussion and interview demonstration**,
not for production trading use.

---

## 🚀 MVP – Tier‑1 Highlights

* **FIX 4.4 connectivity** via QuickFIX/J (Acceptor & Initiator)
* **Deterministic parent/child order model** with FSM-driven lifecycle
* **Explicit OMS → SOR → Venue boundaries** using event envelopes
* **Venue execution pipeline** with matching engine and strategy hooks
* **Seeded, deterministic IDs** via `IdFactory`
* **Deterministic time abstraction** via `DualTimeSource` (enables future replay)
* **End-to-end trace capture** using envelope metadata for execution timing and flow visibility


---

## ❌ Out of Scope (Intentional)

The following are intentionally *not* implemented in this demo:

* Cancel / Replace flows (`FIX 35=F/G`)
* Duplicate FIX idempotency
* Complex order types (IOC, FOK, GTD)
* Production‑grade risk checks or persistence

These are candidates for future extensions, but are excluded to keep the demo focused
on clarity and determinism.

---

## 📊 Architecture at a Glance

The system is organized into three primary layers:

* **OMS (Order Management System)**  
  Owns parent order state, FIX boundaries, and execution lifecycle.

* **SOR (Smart Order Router)**  
  Owns parent-to-child routing decisions and venue dispatch, translating
  parent intents into venue-specific child intents.

* **Venue (Exchange Simulator)**  
  Executes child orders and simulates acknowledgements, fills, matching,
  and market-data-aware execution rules. Venue execution is orchestrated
  by the SOR layer in this demo.

Layer interactions are event-driven, enabling deterministic execution
without cross-layer shared state.

---

## 🔧 Build & Run

**Requirements**

* Java 21+
* Gradle 8+ (wrapper included)

Build:

```bash
./gradlew clean build
```

Run demo (use two terminals):

**Terminal 1 – OMS + SOR + Venue**

```bash
./gradlew runTradeCraft
```

**Terminal 2 – TradeClient**

```bash
./gradlew runTradeClient
```

The TradeClient automatically loads demo orders from `resources/trades-actions.csv`.

---

## 🗂️ Project Structure

```
tradecraft/
 ├─ src/main/java/io/tradecraft/bootstrap        # OMS + SOR + Venue entrypoint
 ├─ src/main/java/io/tradecraft/common           # Domain model, IDs, clocks
 ├─ src/main/java/io/tradecraft/ext/tradeclient  # Trade client (FIX initiator)
 ├─ src/main/java/io/tradecraft/fixqfj           # QuickFIX/J integration
 ├─ src/main/java/io/tradecraft/oms              # OMS core
 ├─ src/main/java/io/tradecraft/sor              # SOR routing and intent handling
 ├─ src/main/java/io/tradecraft/venue            # Venue strategies and matching
 └─ src/main/resources/
```

---

## 🧪 Tests

Run unit tests:

```bash
./gradlew test
```

---

## 📚 Further Documentation

Detailed design notes and deep‑dive material are available in `/docs`, including:

* Architecture and execution flow walkthroughs
* Demo scenarios and expected outcomes
* Logs and execution traces
* Design patterns and trade‑offs
* Replay and latency roadmap

---

## 📜 License

Copyright © 2025 Stanley Wong

Licensed under the Apache License, Version 2.0.

This project is provided for educational and demonstration purposes.
It is not intended to be production‑ready trading software.
