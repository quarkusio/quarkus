### Auto-Generated Manifests

- Kubernetes manifests are generated at build time in `target/kubernetes/`.
- Supports Deployment, Service, Ingress generation.

### Configuration

- `quarkus.kubernetes.replicas=3` — number of replicas.
- `quarkus.kubernetes.env.vars.MY_VAR=value` — environment variables.
- `quarkus.kubernetes.resources.requests.cpu=100m` — resource requests.
- `quarkus.kubernetes.liveness-probe.http-action-path=/q/health/live` — probe config.

### Deployment

- Deploy with `quarkus.kubernetes.deploy=true` during build.
- Requires a container image extension (Docker, Jib, etc.) for building images.

### Common Pitfalls

- Manifests are generated at build time — they reflect the config at build, not runtime.
- Always review generated manifests before deploying to production.
