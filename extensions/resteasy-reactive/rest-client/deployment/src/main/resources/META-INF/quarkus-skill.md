### Defining a REST Client

- Create an interface annotated with `@RegisterRestClient` and JAX-RS annotations.
- Use `@Path`, `@GET`, `@POST`, etc. on methods to define the API contract.
- Return types can be plain objects (Jackson/JSON-B auto-serialized), `Uni<T>` for reactive, or `Response`.

### Configuration

- Set the base URL via config: `quarkus.rest-client."com.example.MyClient".url=http://api.example.com`
- Or use `configKey`: `@RegisterRestClient(configKey = "my-api")` then `quarkus.rest-client.my-api.url=...`
- Timeouts: `quarkus.rest-client.my-api.connect-timeout=5000` and `read-timeout`.

### Injection and Usage

- Inject with `@RestClient MyClient client` — no `@Inject` needed, even if supported.
- The client is a CDI bean — use it in any CDI-managed class.

### Error Handling

- Implement `ResponseExceptionMapper<T>` to convert error responses to exceptions.
- Register with `@RegisterProvider(MyExceptionMapper.class)` on the client interface.

### Testing

- Use `@InjectMock @RestClient` to mock the client in tests.
- Alternatively, use WireMock to simulate the remote API.
- For integration tests, point the client URL to a test server.

### Common Pitfalls

- Do NOT forget to set the URL in config — the client will fail at injection time if missing.
- REST Client uses the REST Reactive stack — do NOT combine with RESTEasy Classic client.
- Always set reasonable timeouts for production use.
