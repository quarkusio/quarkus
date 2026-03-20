### Usage

- Add this extension to auto-generate hypermedia links (HATEOAS) in REST responses.
- Annotate REST methods with `@InjectRestLinks` to add links to responses.
- Use `@RestLink` on resource methods to define link relations.

### Pattern

```java
@Path("/persons")
public class PersonResource {
    @GET
    @RestLink(rel = "list")
    @InjectRestLinks
    public List<Person> list() { ... }

    @GET
    @Path("/{id}")
    @RestLink(rel = "self")
    @InjectRestLinks
    public Person get(@PathParam("id") Long id) { ... }
}
```

### Testing

- Use REST Assured and verify the `_links` or `Link` headers in responses.

### Common Pitfalls

- Entities must have an `id` field (or `@ResourceId` annotation) for link generation to work.
- Links are added via HAL or HTTP `Link` headers depending on the `Accept` header.
