## VenueListener Pattern

### 📡 Pattern: Observer / Event Listener

The `VenueListener` is an implementation of the **Observer Pattern**, used to decouple the venue logic from its downstream effects.

### 🏗️ Structure

- **Subject**: `Venue` or `VenueSupport`
- **Observer**: `VenueListener` interface
- **Events**: `onAck(...)`, `onFill(...)`, `onReject(...)`, etc.
- **Invocation**: Triggered from `VenueExecution` processing

```java
public interface VenueListener {
    void onAck(VenueAck ack);
    void onFill(VenueFill fill);
    void onReject(VenueReject reject);
    ...
}
```

### ✅ Benefits

| Feature             | Description                                                 |
|---------------------|-------------------------------------------------------------|
| **Decoupling**      | Venue logic doesn't care who consumes emitted events        |
| **Flexibility**     | Swap listener behavior (e.g., send to Kafka, log, metric)   |
| **Testability**     | Listeners can be mocked or inspected in unit tests          |
| **Composable**      | Allows layering of observability, metrics, and side effects |

### 🔁 Related Patterns

- **Outbox Pattern**: emit to a durable event bus
- **Event Sourcing**: record/replay `VenueExecution`
- **Publisher-Subscriber**: support multiple listeners (e.g., logging + metrics)

### 🧩 Example

```java
VenueSupport support = new VenueSupport(listener, clock, idFactory);
VenueStrategy strategy = new ImmediateFillStrategy(support);

VenueListener listener = new OutboxListener(eventBus); // Observer
```

