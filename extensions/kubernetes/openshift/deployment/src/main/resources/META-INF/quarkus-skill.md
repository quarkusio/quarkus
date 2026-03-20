### OpenShift Manifests

- Auto-generates OpenShift-specific manifests (DeploymentConfig, Route, BuildConfig).
- Place `quarkus.openshift.*` config for OpenShift-specific settings.

### S2I Builds

- Use `quarkus.openshift.build-strategy=docker` or `s2i`.
- Deploy directly: `quarkus.openshift.deploy=true`.

### Routes

- Routes are auto-generated. Configure with `quarkus.openshift.route.expose=true`.
- TLS: `quarkus.openshift.route.tls.*` for edge/passthrough/reencrypt.

### Common Pitfalls

- OpenShift extension generates OpenShift manifests instead of vanilla Kubernetes.
- Review generated manifests in `target/kubernetes/`.
