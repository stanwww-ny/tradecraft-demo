# Architecture Overview

This document describes the high-level architecture and design intent of the
TradeCraft demo platform.

## Design Goals

The system is designed to prioritize:

- Deterministic execution
- Clear ownership boundaries between layers
- Explicit, event-driven communication
- Foundations for future replay and observability

The demo intentionally avoids production complexity in favor of architectural clarity.

---

## System Layers

### OMS (Order Management System)

The OMS owns:

- Parent order state and lifecycle
- FIX protocol boundaries
- Translation from FIX messages into internal domain events

The OMS is responsible for validating inbound client requests and managing
parent-level order state.

---

### SOR (Smart Order Router)

The SOR owns:

- Parent-to-child routing decisions
- Child order orchestration
- Dispatch of child orders to venues

In this demo, routing logic is intentionally simple and deterministic.
The SOR acts as the coordinator between OMS intent and venue execution.

---

### Venue (Exchange Simulator)

The Venue simulates:

- Order acknowledgements
- Matching and fills
- Market-aware execution behavior

Venue execution is orchestrated by the SOR layer in this demo. The venue does
not maintain global system state outside its execution responsibilities.

---

## Event-Driven Boundaries

Each layer communicates via explicit domain events rather than shared mutable
state. This enforces clear ownership, simplifies reasoning about execution,
and enables deterministic behavior.

Internally, each layer processes events using a single-threaded execution
pipeline with explicit queue ownership. This simplifies concurrency reasoning
and supports deterministic execution, without exposing threading or queue
mechanics across layer boundaries.

---

## Demo Scope

To keep the demo focused and readable, the following constraints apply:

- Single venue
- Single routing decision per parent order
- No persistence
- No cancel or replace flows

These constraints are intentional and align with the educational goals of the project.
