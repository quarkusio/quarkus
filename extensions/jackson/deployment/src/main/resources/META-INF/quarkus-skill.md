### Usage

- This extension provides Jackson ObjectMapper configuration for Quarkus.
- It is automatically included when you add `quarkus-rest-jackson`.
- Customize the ObjectMapper by producing a CDI bean of type `ObjectMapperCustomizer`.

### Customization

```java
@ApplicationScoped
public class MyCustomizer implements ObjectMapperCustomizer {
    @Override
    public void customize(ObjectMapper mapper) {
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }
}
```

### Testing

- Jackson serialization is used automatically in `@QuarkusTest` — no special setup needed.
- Inject `ObjectMapper` in tests for manual serialization assertions.

### Common Pitfalls

- Do NOT create a new `ObjectMapper` manually — inject the CDI-managed one or use `ObjectMapperCustomizer`.
- Do NOT add both Jackson and JSON-B extensions for the same purpose — choose one serialization library.
