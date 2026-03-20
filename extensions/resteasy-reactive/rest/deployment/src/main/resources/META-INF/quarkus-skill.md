### REST Endpoints

- Annotate resource classes with `@Path` and use `@GET`, `@POST`, `@PUT`, `@DELETE` for HTTP methods.
- Return `String` or `Response` for plain text. Add `rest-jackson` or `rest-jsonb` for JSON serialization.
- Use `@PathParam`, `@QueryParam`, `@HeaderParam` for parameter binding.
- For reactive endpoints, return `Uni<T>` or `Multi<T>` from Mutiny.
- For custom status codes, return `RestResponse<T>` (type-safe) or `Response`.
- Inject services with `@Inject`. Keep business logic out of resource classes.

### Filters and Exception Mapping

- Use `@ServerRequestFilter` and `@ServerResponseFilter` on methods for simple filters.
- Use `@ServerExceptionMapper` on methods in resource classes to map exceptions to responses.
- Both approaches are simpler than implementing `ContainerRequestFilter` or `ExceptionMapper<T>`.

### Testing

- Use `@QuarkusTest` with REST Assured for endpoint testing.
- Use `@TestHTTPEndpoint(MyResource.class)` to avoid hardcoding paths.
- Test both success and error paths.
- Test classes and methods should have package-private visibility.

### Common Pitfalls

- Do NOT use `@Singleton` on resource classes — they are request-scoped by default.
- `@Path` on class + `@Path` on methods combine (e.g. `/api` + `/items` = `/api/items`).
- REST Reactive (this extension) is NOT compatible with RESTEasy Classic — do not mix them.
