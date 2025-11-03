Trader/SOR
│
▼
┌──────────┐        (Port)
│  Venue   │  onCommand(VenueCommand)
└────┬─────┘
     │ orchestrates
     ▼
┌───────────┐   uses    ┌─────────────────────┐
│ Strategies├──────────►│ VenueOrderRepository│
│ (Brain)   │           │  (InMemory… stores  │
└────┬──────┘           │   VenueOrder)       │
     │                  └─────────────────────┘
     │ match/risk
     ▼
[ Matching Engine ]  — guarded by →  [ ThreadGuard ]
     │                           (Bouncer)
     ▼
emit Exec/ACK  ─────────────►  Event Bus/Outbox (Herald)
     ▲
     │ time
DualTimeSource (Clock)