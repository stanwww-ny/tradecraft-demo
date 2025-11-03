┌────────────────────────────┐
│  Venue receives cmd        │
└────────────┬───────────────┘
             │
invokes strategy pipeline
             ▼
┌─────────────────────────────────────────────────────────────┐
│ FatFingerRiskStrategy                                       │
│  ✔ appliesTo(NewChildCmd)                                   │
│  ✘ Reject if price too far from NBBO midpoint               │
└─────────────────────────────────────────────────────────────┘
             ▼ (if not rejected)
┌─────────────────────────────────────────────────────────────┐
│ ImmediateFillStrategy                                       │
│  ✔ appliesTo(NewChildCmd)                                   │
│  ✔ Match immediately if IOC or Market                       │
│  ✘ If filled → emit fill                                    │
│  ✘ If not filled → skip to next                             │
└─────────────────────────────────────────────────────────────┘
             ▼
┌─────────────────────────────────────────────────────────────┐
│ MatchingEngineStrategy                                      │
│  ✔ appliesTo(New, Cancel, Replace)                          │
│  → forwards to MatchingEngine.onCommand(cmd)                │
└─────────────────────────────────────────────────────────────┘