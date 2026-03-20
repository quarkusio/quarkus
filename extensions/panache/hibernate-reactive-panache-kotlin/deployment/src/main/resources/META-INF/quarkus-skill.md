### Usage

- This is the Kotlin version of Hibernate Reactive with Panache — for reactive database access in Kotlin projects.
- Extend `PanacheEntity` from `io.quarkus.hibernate.reactive.panache.kotlin` for Active Record pattern.
- All database operations return `Uni<T>` — use Mutiny operators.

### Kotlin-Specific Patterns

```kotlin
@Entity
class Person : PanacheEntity() {
    lateinit var name: String
    var age: Int = 0

    companion object {
        fun findByName(name: String): Uni<Person?> = find("name", name).firstResult()
    }
}
```

### Testing

- Use `@QuarkusTest` with the reactive test infrastructure.
- Dev Services provides a database container automatically.

### Common Pitfalls

- Do NOT block on `Uni` — always chain operations reactively.
- Do NOT use the blocking Panache extensions (`hibernate-orm-panache-kotlin`) with reactive datasources.
- Use `Panache.withTransaction { ... }` instead of `@Transactional` in reactive code.
