### Usage

- Add this extension for JSON-B (Jakarta JSON Binding) support in Quarkus.
- Customize serialization with `@JsonbProperty`, `@JsonbTransient`, `@JsonbDateFormat`, etc.
- Customize the `Jsonb` instance by producing a CDI bean of type `JsonbConfigCustomizer`.

### Customization

```java
@ApplicationScoped
public class MyJsonbCustomizer implements JsonbConfigCustomizer {
    @Override
    public void customize(JsonbConfig config) {
        config.withPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CASE_WITH_UNDERSCORES);
    }
}
```

### Testing

- JSON-B serialization works automatically in `@QuarkusTest`.

### Common Pitfalls

- Do NOT add both JSON-B and Jackson extensions for the same purpose — choose one.
- Jackson is generally preferred in Quarkus for its richer feature set.
- JSON-B annotations (`@JsonbProperty`) are NOT compatible with Jackson annotations (`@JsonProperty`).
