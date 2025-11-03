
## Core Patterns

| Component                   | Pattern Used                         | Role / Responsibility                                    |
|-----------------------------|--------------------------------------|----------------------------------------------------------|
| `MatchingEngine`            | **Command Dispatcher**               | Handles `onCommand(cmd)` for New, Cancel, Replace        |
| `SimpleOrderBook`           | **Stateful Order Book**              | One per side (BID/ASK); price-sorted, FIFO per level     |
| `OrderBook` interface       | **Strategy or Policy Injection**     | Allows alternative book implementations                  |
| Matching loop               | **Greedy Matching** (while fillable) | Loop through contra book until exhausted or IOC stops    |
| PriceLevel → Queue<Order>   | **TreeMap + Queue**                  | Sorted map for price levels, FIFO for time priority      |
| `ThreadGuard`               | **Concurrency Barrier**              | Ensures all matching logic is thread-safe                |
| `VenueExecution`            | **Command Result / Value Object**    | Encapsulates acks, fills, rejects                        |

## Order Book Pattern

Each side (BID/ASK) uses a:

```java
NavigableMap<Price, Queue<RestingRef>> // TreeMap for price, Queue for time
```

- For `buy`: prices sorted **descending**
- For `sell`: prices sorted **ascending**
- Each `Queue` maintains **FIFO** time priority

## Matching Loop Execution Flow

> **Pattern**: `Price → FIFO` → This is a **Price-Time Priority Book**  
Used by most real-world exchanges (e.g., Nasdaq, CME)

```text
onCommand(NewChildCmd):
│
├──> Wrap order into RestingRef
│
├──> Lock ThreadGuard
│     ├──> Attempt match against contra book:
│     │      while (bestPrice crosses && IOC not violated)
│     │          match one or many
│     │          create VenueFill
│     │          update leavesQty
│     │
│     └──> If leavesQty > 0 → book order
│
└──> Return VenueExecution (acks + fills or full fill)
```

