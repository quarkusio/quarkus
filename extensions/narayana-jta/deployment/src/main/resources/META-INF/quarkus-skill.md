
### Declarative Transactions

```java
@Transactional
public void transfer(Long fromId, Long toId, BigDecimal amount) {
    Account from = Account.findById(fromId);
    Account to = Account.findById(toId);
    from.balance = from.balance.subtract(amount);
    to.balance = to.balance.add(amount);
    // commits on success, rolls back on RuntimeException
}
```

- `@Transactional` on CDI bean methods (including REST endpoints) starts a JTA transaction.
- `RuntimeException` crossing the transaction boundary causes automatic rollback.
- Checked exceptions do NOT cause rollback by default.

### Transaction Types

```java
@Transactional(REQUIRED)       // default — join existing or start new
@Transactional(REQUIRES_NEW)   // always start a new transaction (suspends existing)
@Transactional(MANDATORY)      // must be called within an existing transaction
@Transactional(SUPPORTS)       // join if exists, run without if not
@Transactional(NOT_SUPPORTED)  // suspend existing transaction
@Transactional(NEVER)          // fail if a transaction is active
```

### Rollback Rules

```java
@Transactional(rollbackOn = BusinessException.class)        // roll back on this checked exception
@Transactional(dontRollbackOn = WarningException.class)     // don't roll back on this
```

### Programmatic Rollback

```java
@Inject TransactionManager tm;

@Transactional
public void process() {
    if (somethingWrong) {
        tm.setRollbackOnly();  // mark for rollback without throwing
    }
}
```

### QuarkusTransaction API

Programmatic transaction boundaries without `@Transactional`:

```java
import io.quarkus.narayana.jta.QuarkusTransaction;

// Run in a new transaction
QuarkusTransaction.requiringNew().run(() -> {
    Account account = new Account("Alice", BigDecimal.valueOf(1000));
    account.persist();
});

// Run and return a value
Account result = QuarkusTransaction.requiringNew().call(() -> {
    Account account = new Account("Bob", BigDecimal.valueOf(500));
    account.persist();
    return account;
});

// Join existing or start new
QuarkusTransaction.joiningExisting().run(() -> { ... });
```

- `requiringNew()` — always starts a new transaction.
- `joiningExisting()` — joins an active transaction or starts a new one.
- `.run(Runnable)` for void operations, `.call(Callable)` for returning values.

### Transaction Timeout

```java
@Transactional
@TransactionConfiguration(timeout = 10)  // 10 seconds
public void slowOperation() { ... }
```

Or globally: `quarkus.transaction-manager.default-transaction-timeout=30s`

### Testing Transactions

- Use `@TestTransaction` on test methods — the transaction is rolled back after each test for isolation.
- `@Transactional` works in `@QuarkusTest` tests.
- To verify rollback, perform an operation that should fail, then query to confirm data wasn't persisted.

### Common Pitfalls

- `@Transactional` only works on CDI bean methods — not on private methods or direct method calls within the same class (self-invocation bypasses the interceptor).
- `RuntimeException` = automatic rollback. Checked exception = NO rollback (unless `rollbackOn` is set).
- `REQUIRES_NEW` suspends the outer transaction — changes in the inner transaction persist even if the outer rolls back. Use this for audit logs.
- `MANDATORY` throws `TransactionRequiredException` if no active transaction exists — useful for service methods that must be called from a transactional context.
- `QuarkusTransaction` is preferred over `UserTransaction` for programmatic boundaries — it's more concise and Quarkus-native.
