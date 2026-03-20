### Usage

- Add this extension to use JAXB (XML) serialization with the REST Client.
- Annotate DTOs with `@XmlRootElement` and JAXB annotations.
- Set `@Consumes(MediaType.APPLICATION_XML)` and `@Produces(MediaType.APPLICATION_XML)` on client methods.

### Testing

- Use `@QuarkusTest` with WireMock for mocking XML APIs.

### Common Pitfalls

- Ensure DTOs have JAXB annotations — without `@XmlRootElement`, serialization will fail.
- For JSON APIs, use `rest-client-jackson` or `rest-client-jsonb` instead.
