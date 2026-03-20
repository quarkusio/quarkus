### Usage

- Add this extension to enable HAL (Hypertext Application Language) JSON responses from REST endpoints.
- Return `HalEntityWrapper` or `HalCollectionWrapper` from REST methods to produce HAL-formatted JSON.
- Alternatively, use `@Produces({"application/hal+json"})` and the extension wraps responses automatically when used with REST Links.

### Pattern

```java
@GET
@Produces({"application/hal+json", "application/json"})
@RestLink(rel = "list")
@InjectRestLinks
public List<Person> list() { ... }
```

### Testing

- Use REST Assured with `Accept: application/hal+json` header and assert on `_links` and `_embedded` fields.

### Common Pitfalls

- HAL is a hypermedia format — it works best with `quarkus-rest-links` for automatic link generation.
- Clients must request `application/hal+json` — regular `application/json` returns standard JSON.
