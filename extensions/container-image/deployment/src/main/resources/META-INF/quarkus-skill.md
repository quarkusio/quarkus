
### Building a Container Image

Trigger a container image build during the Quarkus build:

```bash
./mvnw package -Dquarkus.container-image.build=true
```

Or push directly to a registry:

```bash
./mvnw package -Dquarkus.container-image.build=true -Dquarkus.container-image.push=true
```

### Image Naming

```properties
quarkus.container-image.group=myorg
quarkus.container-image.name=myapp
quarkus.container-image.tag=1.0
quarkus.container-image.registry=quay.io
# Result: quay.io/myorg/myapp:1.0

# Or set the full image string (overrides group/name/tag/registry)
quarkus.container-image.image=quay.io/myorg/myapp:1.0

# Additional tags
quarkus.container-image.additional-tags=latest,stable
```

Defaults: `group` = `${user.name}`, `name` = application name, `tag` = application version.

### Builder Selection

Add **one** builder extension to choose the build strategy:

| Extension | Builder | Use When |
|-----------|---------|----------|
| `container-image-docker` | Docker daemon | Docker installed locally |
| `container-image-podman` | Podman | Podman preferred |
| `container-image-jib` | Jib (no daemon) | No Docker/Podman available |
| `container-image-buildpack` | Cloud Native Buildpacks | Using buildpacks |
| `container-image-openshift` | OpenShift S2I | Deploying to OpenShift |

If multiple builders are present, select one:
```properties
quarkus.container-image.builder=docker
```

### Registry Authentication

```properties
quarkus.container-image.username=myuser
quarkus.container-image.password=mypass
quarkus.container-image.insecure=false
```

### Custom Labels

```properties
quarkus.container-image.labels.maintainer=team@example.com
quarkus.container-image.labels.version=1.0
```

### Common Pitfalls

- **Build is not triggered by default**: You must pass `-Dquarkus.container-image.build=true` or set the property. Just adding the extension doesn't build an image.
- **Builder extension required**: The `container-image` extension alone doesn't build anything — you need a builder extension (docker, jib, podman, etc.).
- **Jib doesn't need Docker**: `container-image-jib` builds images without a local Docker daemon — useful in CI environments.
- **Push requires authentication**: Set `username`/`password` or use Docker/Podman credential helpers.
- **Tag defaults to version**: If no tag is set, the application version is used. Set `quarkus.container-image.tag` explicitly for non-version tags like `latest`.
