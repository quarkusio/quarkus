### Building Container Images

- Build: `./mvnw package -Dquarkus.container-image.build=true`.
- Push: `./mvnw package -Dquarkus.container-image.push=true`.

### Configuration

- `quarkus.container-image.group=myorg` — image group/namespace.
- `quarkus.container-image.name=myapp` — image name (default: artifactId).
- `quarkus.container-image.tag=1.0` — image tag (default: project version).
- `quarkus.container-image.registry=ghcr.io` — registry.

### Custom Dockerfile

- Place custom `Dockerfile.jvm` or `Dockerfile.native` in `src/main/docker/`.

### Common Pitfalls

- Docker must be running to build images.
- The default Dockerfile uses the fast-jar format.
