
### Usage

Add the extension to use `application.yaml` (or `application.yml`) instead of or alongside `application.properties`. Place the file in `src/main/resources/`.

```yaml
quarkus:
  http:
    port: 8080
  datasource:
    db-kind: postgresql
    username: admin
    password: secret
    jdbc:
      url: jdbc:postgresql://localhost:5432/mydb

greeting:
  message: Hello
  name: World
```

### Profile-Specific Configuration

Use `%profile` prefix:

```yaml
"%dev":
  quarkus:
    datasource:
      jdbc:
        url: jdbc:h2:mem:devdb

"%test":
  quarkus:
    datasource:
      jdbc:
        url: jdbc:h2:mem:testdb
```

Note the quotes around `%dev` — YAML requires them because `%` is a special character.

### Both Formats Coexist

`application.properties` and `application.yaml` can exist simultaneously. Properties from both are merged, with `application.yaml` taking precedence for duplicate keys.

### Common Pitfalls

- **Profile prefix needs quotes**: `%dev` must be `"%dev"` in YAML — without quotes, YAML parsers may reject the file.
- **Precedence**: `application.yaml` overrides `application.properties` for the same key. System properties and environment variables override both.
- **Nested keys**: The YAML structure must mirror the dot-separated property path. `quarkus.http.port` becomes `quarkus: http: port:`.
- **Lists**: Use YAML array syntax for list properties: `quarkus.http.cors.origins` becomes a YAML list under `origins:`.
- **No special features**: This extension only adds YAML parsing support. All config semantics (profiles, expressions, defaults) work identically to properties files.
