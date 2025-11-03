# 🧩 OMS FIX Acceptor — Thread & Architecture Notes

This document explains how `OmsFixServerModule` launches a QuickFIX/J `SocketAcceptor`,
and what happens under the hood when multiple FIX sessions are connected.

---

## 1. Overview

`OmsFixServerModule` is responsible for bootstrapping a FIX acceptor that listens for
incoming trader connections (FIX 4.2/4.4, `FIX.4x` sessions).  
Internally, it uses **QuickFIX/J**, which builds upon **Apache MINA** — a non-blocking I/O
framework based on Java’s NIO selectors.

When `OmsFixServerModule` calls:

```java
SocketAcceptor acceptor =
    new SocketAcceptor(app, storeFactory, settings, logFactory, messageFactory);
acceptor.start();
```

QuickFIX/J wires up the following threads automatically.

---

## 2. Thread Model

| Layer / Thread Type | Responsibility | Typical Count | Notes |
|----------------------|----------------|----------------|-------|
| **Acceptor Thread** | Listens on TCP port and accepts new connections | 1 | One per `SocketAcceptor` |
| **MINA I/O Processor Threads** | Handle socket reads/writes using NIO selectors | ~1–2 | Shared across all sessions |
| **FIX Message Dispatcher** | Parses FIX messages and calls `Application.onMessage()` | **1 shared thread** | Sequential delivery across all sessions |
| **Timer / Scheduler Thread** | Heartbeats, session resets, timeouts | 1 | Internal background timer |

🧠 **Key point:**  
`SocketAcceptor` uses a **single dispatcher thread** to process all FIX sessions,
ensuring deterministic order of message delivery (no interleaving between sessions).

---

## 3. Apache MINA Explained

Apache MINA handles **low-level I/O multiplexing**:

```
[SocketAcceptor] → [MINA NIO Selector] → [I/O Threads] → [FIX Message Dispatcher]
```

- MINA manages non-blocking sockets for all sessions.
- It decodes/encodes FIX frames.
- It enqueues parsed messages to the dispatcher thread.
- The dispatcher invokes your `Application` (here: `OmsFixInbound.onMessage()`).

This separation allows TradeCraft to focus purely on **domain translation**
(`FixInboundMapper`, `FixInboundBridge`) rather than network I/O details.

---

## 4. ThreadedSocketAcceptor (optional variant)

For higher concurrency, QuickFIX/J offers:

```java
ThreadedSocketAcceptor acceptor =
    new ThreadedSocketAcceptor(app, storeFactory, settings, logFactory, messageFactory);
```

This version spawns **one message-processing thread per FIX session**, enabling
independent message processing between sessions (useful for many traders or venues).

| Acceptor Type | Dispatcher Threads | Determinism | Throughput |
|----------------|-------------------|--------------|-------------|
| `SocketAcceptor` | 1 (shared) | ✅ Deterministic ordering | ⚠️ May bottleneck under load |
| `ThreadedSocketAcceptor` | 1 per session | Slightly relaxed ordering | 🚀 Parallel session throughput |

TradeCraft’s MVP-1 intentionally uses `SocketAcceptor` to maintain **causal determinism**
and simplify event replay.

---

## 5. Visual Architecture

```mermaid
flowchart LR
    A[Trader FIX Client] -->|Logon/NewOrderSingle| B[SocketAcceptor]
    B --> C[Apache MINA I/O Threads]
    C --> D[QuickFIX/J Dispatcher<br>(single-threaded)]
    D --> E[OmsFixInbound.onMessage()]
    E --> F[FixInboundMapper ➜ EvBound ➜ Pipeline]
```

---

## 6. Design Choice in TradeCraft

- ✅ **Determinism:** All FIX messages enter the OMS via one ordered pipeline.
- ✅ **Thread Safety:** No shared mutable state across threads in the FIX layer.
- ✅ **Replayability:** Single-threaded dispatcher aligns with dual-time replay model.
- 🔜 **Future Upgrade:** When scaling to many concurrent traders, swap to
  `ThreadedSocketAcceptor` + per-session `Executor`.

---

## 7. References

- [QuickFIX/J GitHub — SocketAcceptor](https://github.com/quickfix-j/quickfixj)
- [Apache MINA NIO Overview](https://mina.apache.org)
- TradeCraft Source: `io.tradecraft.oms.fixqfj.OmsFixServerModule`

---

**Summary**

> In TradeCraft, `OmsFixServerModule` runs a QuickFIX/J `SocketAcceptor` built on Apache MINA.
> It uses a single message-dispatch thread for all sessions, ensuring causality and reproducibility —
> a deliberate design choice for low-latency and deterministic OMS event replay.
