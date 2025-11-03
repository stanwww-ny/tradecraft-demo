# 🧭 NBBO and Pricing Rules — How the Venue Reacts to the Market

> **Pattern:** `NBBO → Reactor → Strategy(RuleChain) → Effect(Fill/Ack)`  
> **Applies to:** `DefaultVenue`, `ImmediateFillStrategy`, `CrossableLimitTopOfBookRule`, `TopOfBookRule`

---

## 1️⃣ The Whisper of the Market

The Venue is usually quiet — it only acts when told to.

But sometimes, the **market speaks first**.

When the *National Best Bid and Offer (NBBO)* changes,  
the Venue hears a **whisper** from the outside world:

> “Hey, the price just moved.”

This whisper — the `NbboUpdate` —  
is what wakes the Venue and triggers evaluation of its strategies.

That’s why NBBO is called an **external reactor**:  
it can wake the Venue even when no new orders arrive.

---

## 2️⃣ The Two Little Rules

Inside the `ImmediateFillStrategy`,  
two small but important rule workers decide what to do:

| Rule | Purpose | When It Triggers |
|------|----------|------------------|
| **CrossableLimitTopOfBookRule** | Fills *limit* orders that cross NBBO (buy ≥ ask, sell ≤ bid). | When a limit order becomes marketable. |
| **TopOfBookRule** | Fills *market* orders at NBBO top of book. | When both bid and ask exist. |

They live inside the **Rule Chain**:

```java
this.priceRules = List.of(
    new CrossableLimitTopOfBookRule(support.nbboProvider()),
    new TopOfBookRule(support.nbboProvider())
);
```

Each rule asks a simple question:  
“Given this NBBO, can I fill this order *now*?”

The first rule to answer **yes** decides the price and creates the fill.

---

## 3️⃣ One Snapshot, One Truth

Before any rule makes a decision,  
the Venue takes a **snapshot** of the current NBBO:

```java
var snap = support.nbboProvider().snapshot();
```

All rules share this same frozen snapshot.  
That way, every decision is based on **one consistent view** of the market.

Even if another NBBO update arrives mid-decision,  
it won’t affect the outcome.

> 🧩 **Result:** Determinism.  
> Same NBBO snapshot + same order → same decision in replay.

---

## 4️⃣ The Idempotency Spell

Sometimes, the same whisper repeats —  
the same NBBO or order command gets reprocessed.

Without protection, the Venue might fill twice.  
So it keeps a **short-term ledger** of fills already seen:

```java
(childId, qty, price, source)
```

If the same combination shows up again,  
the Venue quietly says:

> “Already handled.”

and returns `noop()`.

That’s **idempotency** — the magic that ensures each fill happens only once.

---

## 5️⃣ The Chain of Events

```
NBBO update  →  Venue wakes
                 ↓
           ImmediateFillStrategy
                 ↓
         +---------------------- +
         |  Price Rule Chain     |
         |-----------------------|
         |  CrossableLimitRule   |  →  fills if order crosses NBBO
         |  TopOfBookRule        |  →  fills market orders
         +-----------------------+
                 ↓
           Fill / Ack emitted
```

---

## 6️⃣ Example Scenarios

| Event | NBBO | Rule Triggered | Fill Price | Result |
|-------|------|----------------|-------------|---------|
| Market BUY | 195/205 | TopOfBookRule | 205 | Filled immediately |
| Limit BUY 200 | 195/205 | none | – | Rests on book |
| NBBO moves to 199/200 | 199/200 | CrossableLimitRule | 200 | Filled |
| Duplicate NBBO | 199/200 | none | – | Idempotent (noop) |

---

## 7️⃣ Why This Matters

- **Reactivity:** The Venue doesn’t wait for commands — it reacts to market change.
- **Determinism:** Each decision is reproducible under replay.
- **Safety:** Missing NBBOs skip gracefully instead of throwing.
- **Extensibility:** Add new rules (PeggedRule, MidpointRule, FatFingerRule) without touching strategy code.

---

## 8️⃣ The Moral of the Story

> The **market (NBBO)** decides when action is possible.  
> The **rules** interpret what that means.  
> The **strategy** turns it into an effect.  
> The **Venue** records it deterministically.

The Venue never guesses — it just listens.  
It reacts to the world, one NBBO whisper at a time.

---

## 9️⃣ Future Extensions

- 🧩 **PeggedRule** — follow NBBO mid or primary side
- 🧩 **MidpointRule** — midpoint execution
- 🧩 **FatFingerRule** — price-band validation before fill
- 🧩 **RepriceRule** — dynamic adjustment when NBBO widens

Each rule simply plugs into the same `List<PriceRule>` chain —  
keeping the Venue reactive, predictable, and testable.

---

## 🔟 Summary Flow

```
NBBO update (external reactor)
    ↓
NbboStateStore.update()
    ↓
ImmediateFillStrategy.apply(order)
    ↓
PriceRule chain evaluates snapshot
    ↓
First applicable rule decides price
    ↓
FillEffect (Ack + ER) emitted
    ↓
Idempotency window records key
```

---

> **TradeCraft Principle:**  
> “NBBO is not just data — it’s a *signal*.  
> The Venue listens.  
> The Rules interpret.  
> The Strategy acts.  
> The Effect records.”

---
