### Usage

- This is the Kotlin version of Hibernate ORM with Panache — use this instead of `hibernate-orm-panache` in Kotlin projects.
- Extend `PanacheEntity` for Active Record pattern or implement `PanacheRepository<T>` for Repository pattern.
- Use Kotlin companion objects for static finder methods on entities.

### Entity Design

- Use Kotlin data classes or regular classes with `var` properties — Panache enhances property access at build time.
- The `id` field is provided by `PanacheEntity` — do NOT redeclare it.
- For custom ID types, extend `PanacheEntityBase`.

### Kotlin-Specific Patterns

```kotlin
@Entity
class Person : PanacheEntity() {
    lateinit var name: String
    var age: Int = 0

    companion object {
        fun findByName(name: String) = find("name", name).firstResult()
    }
}
```

### Testing

- Use `@QuarkusTest` — Dev Services provides a test database automatically.
- Use `@TestTransaction` to roll back after each test.

### Common Pitfalls

- Do NOT use the Java `hibernate-orm-panache` extension in Kotlin projects — use this Kotlin-specific version.
- Do NOT use `val` for entity fields that need to be mutable — use `var` or `lateinit var`.
- Companion object methods replace Java's static methods on entities.
