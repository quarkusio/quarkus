### Automatic Overload Protection

- Automatically rejects requests when the server is overloaded (returns HTTP 503).
- No code changes needed — just add the extension.

### Priority Classification

- Requests are classified by priority (critical, normal, degraded).
- Higher-priority requests are shed last.

### Configuration

- `quarkus.load-shedding.max-limit` — maximum concurrent requests.
- `quarkus.load-shedding.initial-limit` — initial limit (auto-adjusted).

### Common Pitfalls

- Load shedding is complementary to rate limiting — it protects the server, not the client.
