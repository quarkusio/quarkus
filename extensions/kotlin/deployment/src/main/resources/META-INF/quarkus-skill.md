### Usage

- Add this extension to enable Kotlin support in Quarkus applications.
- Kotlin classes, coroutines, and data classes work out of the box.
- REST endpoints can use suspend functions — they are handled reactively.

### Coroutines

- Use `suspend fun` in REST endpoints — Quarkus handles the coroutine context automatically.
- Return `Flow<T>` for streaming responses.

### Data Classes

- Kotlin data classes work with Jackson serialization out of the box.
- For JPA entities, do NOT use data classes — use regular classes with `var` properties (JPA requires mutable fields and a no-arg constructor).

### Testing

- Use `@QuarkusTest` — all Kotlin test frameworks (JUnit 5 with Kotlin) work normally.
- Use `runBlocking` in tests if testing coroutine-based code directly.

### Common Pitfalls

- Kotlin classes are `final` by default — use the `kotlin-allopen` compiler plugin (auto-configured by Quarkus) for CDI beans.
- Do NOT use Kotlin data classes for JPA entities — they generate `equals`/`hashCode` based on all fields, which conflicts with JPA identity.
- For Panache, use the Kotlin-specific extensions (`hibernate-orm-panache-kotlin`, `mongodb-panache-kotlin`).
