### Usage

- Add this extension to render Qute templates directly from REST endpoints.
- Return `TemplateInstance` from a REST method to render a template as HTML.
- Templates are loaded from `src/main/resources/templates/`.

### Pattern

```java
@Path("/hello")
public class HelloResource {
    @Inject
    Template hello; // matches templates/hello.html

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get(@QueryParam("name") String name) {
        return hello.data("name", name);
    }
}
```

### Testing

- Use `@QuarkusTest` with REST Assured — assert on the rendered HTML content.
- Templates support hot-reload in dev mode.

### Common Pitfalls

- The `@Inject Template` field name must match the template file name (without extension).
- Use `@CheckedTemplate` for type-safe template rendering instead of injecting `Template` directly.
- Do NOT forget `@Produces(MediaType.TEXT_HTML)` — without it, the response may be serialized as JSON.
