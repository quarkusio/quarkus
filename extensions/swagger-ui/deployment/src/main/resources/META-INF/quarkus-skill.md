### Swagger UI

- Available at `/q/swagger-ui` in dev mode — no config needed.
- Requires `quarkus-smallrye-openapi` extension for the OpenAPI spec.

### Production

- Swagger UI is NOT included in production builds by default.
- Enable with `quarkus.swagger-ui.always-include=true`.

### Configuration

- `quarkus.swagger-ui.path=/swagger` — custom path.
- `quarkus.swagger-ui.theme=flattop` — UI theme.

### Common Pitfalls

- Do NOT enable `always-include` in production unless the API is public.
- Swagger UI size increases the final artifact — disable if not needed.
