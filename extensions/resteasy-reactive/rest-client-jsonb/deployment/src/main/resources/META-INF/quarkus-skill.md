### Usage

- Add this extension to use JSON-B serialization with the REST Client.
- JSON serialization/deserialization is automatic with POJOs.

### Testing

- Use `@QuarkusTest` with WireMock for mocking external APIs.

### Common Pitfalls

- Do NOT add both `rest-client-jsonb` and `rest-client-jackson` — choose one serialization library.
- Jackson is generally preferred in Quarkus for its richer feature set.
