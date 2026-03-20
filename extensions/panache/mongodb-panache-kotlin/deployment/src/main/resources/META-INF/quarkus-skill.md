### Usage

- This is the Kotlin version of MongoDB with Panache — use this instead of `mongodb-panache` in Kotlin projects.
- Extend `PanacheMongoEntity` for Active Record pattern or implement `PanacheMongoRepository<T>` for Repository pattern.
- Use Kotlin companion objects for static finder methods.

### Entity Design

```kotlin
class Person : PanacheMongoEntity() {
    lateinit var name: String
    var age: Int = 0

    companion object {
        fun findByName(name: String) = find("name", name).firstResult()
    }
}
```

### Testing

- Use `@QuarkusTest` — Dev Services provides a MongoDB container automatically.

### Common Pitfalls

- Do NOT use the Java `mongodb-panache` extension in Kotlin projects — use this Kotlin-specific version.
- Do NOT use `val` for entity fields that need to be mutable — use `var` or `lateinit var`.
- The `id` field is provided by `PanacheMongoEntity` — do NOT redeclare it.
