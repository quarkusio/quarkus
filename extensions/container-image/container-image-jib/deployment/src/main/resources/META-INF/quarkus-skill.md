### Dockerless Builds

- Jib builds container images without Docker — no Dockerfile or Docker daemon needed.
- Build: `./mvnw package -Dquarkus.container-image.build=true`.

### Configuration

- Same `quarkus.container-image.*` config as Docker extension.
- `quarkus.jib.base-jvm-image` — base image (default: Java 21 distroless).
- `quarkus.jib.ports=8080` — exposed ports.

### Common Pitfalls

- Jib does NOT support native images — use Docker or Podman extension for native.
- Authentication to registries uses Docker config or environment variables.
