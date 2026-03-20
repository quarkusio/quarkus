### Usage

- Add this extension for JAXB (XML binding) support in Quarkus.
- Annotate classes with `@XmlRootElement`, `@XmlElement`, etc.
- Used with `quarkus-rest-jaxb` or `quarkus-rest-client-jaxb` for XML REST endpoints.

### Native Image

- JAXB classes must be registered for reflection in native image builds.
- Quarkus auto-registers classes annotated with JAXB annotations.

### Testing

- Use `@QuarkusTest` — JAXB serialization works automatically with REST Assured.

### Common Pitfalls

- For JSON APIs, use Jackson or JSON-B instead — JAXB is specifically for XML.
- Ensure all JAXB-annotated classes have a no-arg constructor.
