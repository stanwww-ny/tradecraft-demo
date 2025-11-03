# 🌱 ID Generator Storytelling & Design Patterns (id.zip)

## 1. Story: "The Seeded Foundry"
Every trading day, the system opens the **Foundry** that forges identities for orders, executions, and venues.
It must be:
- ⚙️ **Deterministic** — same inputs always yield same IDs.
- 🔄 **Recoverable** — resumes cleanly after crash.
- 🧩 **Composable** — adding new generators never reshuffles existing ones.
- 🧠 **Self-descriptive** — IDs carry their own context (date, run, sequence).

This is achieved through **Seeds**, **Salts**, and **Sequence Stores**.

---

## 2. Visual Overview
```
                      ┌────────────────────────────┐
                      │         runSeed            │
                      │ e.g. 20250927 (per replay) │
                      └──────────┬─────────────────┘
                                 │
         ┌───────────────────────┼────────────────────────────┐
         ▼                       ▼                           ▼
 Seeds.derive("exec-id")   Seeds.derive("venue-id")   Seeds.derive("trader-1")
         │                       │                           │
    execSeed                 venueSeed                   trader1Seed
         │                       │                           │
 SplittableRandom            SplittableRandom             SplittableRandom
   (execRng)                   (venueRng)                   (t1Rng)
```

Each **salt** creates an independent random stream that remains stable across runs.

---

## 3. Key Concepts

| Concept | Description | Pattern |
|----------|--------------|----------|
| **RunSeed** | Master seed per trading day or replay run | Configuration Context |
| **Salt** | Stable string naming each stream (e.g. `"exec-id"`) | Namespace / Labeling |
| **Seeds.derive()** | Deterministic hash mix of `runSeed + salt` | Derivation Function |
| **SplittableRandom** | Deterministic RNG from each derived seed | Factory Method |
| **SequenceAllocator** | Monotonic counter with persistence | Repository / Singleton |
| **IdSource** | Builds readable IDs: `<Kind>-<MIC>-<BizDate>-<Run>-<Seq>` | Builder + Value Object |
| **SequenceStore** | Persists `{run, lastSeq}` for crash recovery | Repository |

---

## 4. Lifecycle of an ID
```
Run starts with runSeed = 20250927
│
├─ Derive execSeed = Seeds.derive(runSeed, "exec-id")
│
├─ Use SplittableRandom(execSeed) to generate deterministic numbers
│
├─ SequenceAllocator persists {run, lastSeq}
│
└─ CompositeIdSource builds: EX-XNAS-20250927-1-000001
```

Crash recovery:
```
Load lastSeq=43 → resume at 44
or mark new run=2 → restart seq=1
```

---

## 5. Replay vs Live
```
┌──────────────────────────┐      ┌──────────────────────────┐
│        LIVE Mode         │      │       REPLAY Mode        │
│ - SystemClock            │      │ - ReplayClock            │
│ - PersistedSeqStore      │      │ - SeededSeqAllocator     │
│ - Record IDs & Time      │      │ - Deterministic re-run   │
└──────────────────────────┘      └──────────────────────────┘
```

*Same runSeed + same salts = identical replay outputs.*

---

## 6. ID Structure
```
<Kind>-<MIC>-<BizDate>-<Run>-<Seq>
Example: EX-XNAS-20250927-1-000001
```

| Field | Meaning |
|--------|----------|
| Kind | e.g., EX (Exec), V (Venue), SOR (Child) |
| MIC | Market identifier code (e.g., XNAS, XNYS) |
| BizDate | Trading date from TradingCalendar |
| Run | Run token incremented on each restart |
| Seq | Sequence number (monotonic) |

---

## 7. Design Patterns Summary

| Category | Pattern | Purpose |
|-----------|----------|----------|
| Determinism | Seed Derivation / Salt Hashing | Stable sub-seeds per label |
| Randomness | SplittableRandom | Deterministic random generation |
| Structure | Builder Pattern | Composable ID structure |
| Persistence | Repository Pattern | Durable sequence recovery |
| Reliability | Persist-then-Publish | Prevent duplicate IDs |
| Replay | Event Sourcing | Deterministic replay from same inputs |
| Namespace | Salt Labeling | Position-independent independence |
| Safety | Idempotency | Avoid reuse or collisions |

---

## 8. Mental Model / Metaphor
```
         🌳 Seed Tree
         ├── trunk: runSeed (daily master)
         ├── branches: salts (exec-id, trader-1, venue-id)
         ├── leaves: SplittableRandom streams
         └── fruits: generated IDs / random outputs
```

- 🌱 **Seed** = foundation of determinism.
- 🏷️ **Salt** = label that namespaces randomness.
- 🎲 **SplittableRandom** = reproducible generator.
- 🔢 **SequenceStore** = durability across restarts.
- 🍎 **ID** = final fruit: unique, readable, traceable.

---

## 9. Key Takeaways

1. **Same runSeed + salt → same sub-seed → same IDs every replay.**
2. **Adding a new salt never reshuffles others.**
3. **Crash recovery via persisted SequenceStore ensures continuity.**
4. **IDs are self-descriptive and auditable.**
5. **System unifies randomness, persistence, and replay into a deterministic Foundry.**