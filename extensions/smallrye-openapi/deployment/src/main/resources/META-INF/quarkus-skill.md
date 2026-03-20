### Auto-Generated API Documentation

- OpenAPI spec is automatically generated from JAX-RS endpoints.
- Available at `/q/openapi` (YAML) or `/q/openapi?format=json`.
- Swagger UI is available at `/q/swagger-ui` in dev mode.

### Annotations

- `@Operation(summary = "...")` — describe an endpoint.
- `@Tag(name = "...")` — group endpoints.
- `@APIResponse(responseCode = "200", description = "...")` — document responses.
- `@Schema` — document request/response models.

### Configuration

- `quarkus.smallrye-openapi.info-title` — API title.
- `quarkus.smallrye-openapi.info-version` — API version.
- `quarkus.smallrye-openapi.info-description` — API description.

### Static OpenAPI

- Place a static `openapi.yml` or `openapi.json` in `META-INF` to merge with generated spec.

### Common Pitfalls

- Annotations are optional — the spec is generated from JAX-RS annotations automatically.
- Use `quarkus.swagger-ui.always-include=true` to enable Swagger UI in production (disabled by default).
