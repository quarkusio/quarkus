### Usage

- Add this extension to use Jackson serialization with the REST Client.
- JSON serialization/deserialization is automatic — define your REST client interface with POJOs.

### REST Client Pattern

```java
@RegisterRestClient(configKey = "my-api")
public interface MyApiClient {
    @GET
    @Path("/items")
    List<Item> getItems();

    @POST
    @Path("/items")
    Item createItem(Item item);
}
```

### Testing

- Use `@QuarkusTest` with `@InjectMock @RestClient` to mock REST clients, or WireMock for mocking external APIs.
- Inject the REST client with `@Inject @RestClient`.

### Common Pitfalls

- Do NOT add both `rest-client-jackson` and `rest-client-jsonb` — choose one serialization library.
- Configure the base URL with `quarkus.rest-client.my-api.url` (matching the `configKey`).
