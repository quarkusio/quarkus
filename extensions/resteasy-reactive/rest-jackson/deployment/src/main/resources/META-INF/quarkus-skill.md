### REST Endpoints

- Annotate resource classes with `@Path` and use `@GET`, `@POST`, `@PUT`, `@DELETE` for HTTP methods.
- Place `@Consumes(MediaType.APPLICATION_JSON)` and `@Produces(MediaType.APPLICATION_JSON)` at the class level.
- Use plural nouns for resource paths (e.g. `/greetings`, `/items`).
- Return domain objects directly — Jackson serialization is automatic. Avoid wrapping in `Response` unless you need custom status codes or headers.
- For custom status codes, return `RestResponse<T>` (type-safe) or use `Response`.
- Inject services with `@Inject`. Keep business logic out of resource classes.
- Use `@PathParam`, `@QueryParam`, `@HeaderParam` for parameter binding.
- For reactive endpoints, return `Uni<T>` or `Multi<T>` from Mutiny.

### Request/Response Filtering

- Use `@ServerRequestFilter` and `@ServerResponseFilter` annotations on methods for simple filters.
- For complex filters, implement `ContainerRequestFilter` / `ContainerResponseFilter`.

### Jackson Configuration

- Create a `@Singleton` bean implementing `ObjectMapperCustomizer` to customize Jackson.
- For per-endpoint customization, use `@CustomSerialization`.
- Records work well as request/response DTOs — Jackson handles them automatically.

### Validation

- Add the `quarkus-hibernate-validator` extension for Bean Validation.
- Annotate parameters or request body fields with `@NotNull`, `@Size`, `@Valid`, etc.
- Validation errors automatically return 400 with details.

### Error Handling

- Use `@ServerExceptionMapper` annotation on methods in resource classes to map exceptions to responses.
- Alternatively, implement `ExceptionMapper<T>` as a CDI bean.

### Testing

- Use `@QuarkusTest` for integration tests that start the full application.
- Use REST Assured for HTTP endpoint testing:
  ```java
  @QuarkusTest
  class GreetingResourceTest {
      @Test
      void testGetEndpoint() {
          given()
              .when().get("/greetings")
              .then()
              .statusCode(200)
              .body("$.size()", greaterThan(0));
      }

      @Test
      void testPostEndpoint() {
          given()
              .contentType(ContentType.JSON)
              .body(new Greeting("hello"))
              .when().post("/greetings")
              .then()
              .statusCode(201);
      }
  }
  ```
- Test classes and methods should have package-private visibility (no `public`).
- Always test both success and error paths (invalid input, not found, etc.).
- Use `@TestHTTPEndpoint(GreetingResource.class)` to avoid hardcoding paths.
- For testing with authentication, use `@TestSecurity(user = "admin", roles = "admin")`.

### Common Pitfalls

- Do NOT use `@Singleton` on resource classes — they are request-scoped by default, which is correct.
- Do NOT register Jackson modules via `META-INF/services` — use the CDI-based `ObjectMapperCustomizer`.
- Empty `@Path("")` on a class combined with `@Path` on methods is valid and common for sub-resources.
