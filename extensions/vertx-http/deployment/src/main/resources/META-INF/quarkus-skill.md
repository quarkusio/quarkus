### Usage

- This extension provides the HTTP server layer for Quarkus, powered by Eclipse Vert.x.
- It is automatically included — you rarely need to add it explicitly.
- Configure HTTP port with `quarkus.http.port` (default: 8080).

### Routing

- Use `@Route` annotation for reactive Vert.x routes as an alternative to Jakarta REST.
- Configure CORS with `quarkus.http.cors=true` and `quarkus.http.cors.origins`.
- Serve static resources from `src/main/resources/META-INF/resources/`.

### SSL/TLS

- Configure with `quarkus.http.ssl.certificate.files` and `quarkus.http.ssl.certificate.key-files`.
- Use the TLS registry extension for more advanced TLS configuration.

### Testing

- Use `@QuarkusTest` — the HTTP server starts automatically on a random test port.
- REST Assured is pre-configured with the correct test port.

### Common Pitfalls

- Do NOT set `quarkus.http.port` in tests — Quarkus uses a random port automatically.
- For production, configure `quarkus.http.host=0.0.0.0` to bind to all interfaces (default is `localhost`).
