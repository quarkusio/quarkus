### Declarative Transactions

- Use `@Transactional` on CDI bean methods.
- `@Transactional(REQUIRED)` (default) — join existing or create new.
- `@Transactional(REQUIRES_NEW)` — always create new.
- `@Transactional(NOT_SUPPORTED)` — suspend current transaction.

### Programmatic Transactions

- Use `QuarkusTransaction.requiringNew().run(() -> ...)` for programmatic control.
- Or inject `UserTransaction` for JTA-standard API.

### Configuration

- `quarkus.transaction-manager.default-transaction-timeout=60s` — global timeout.

### Common Pitfalls

- `@Transactional` only works on CDI bean methods — not private methods or plain classes.
- Do NOT use `@Transactional` with Hibernate Reactive — use `@WithTransaction` instead.
