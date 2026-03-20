### Usage

- Add this extension to read configuration from Kubernetes ConfigMaps and Secrets.
- Configure with `quarkus.kubernetes-config.config-maps=my-configmap` and/or `quarkus.kubernetes-config.secrets=my-secret`.
- Properties from ConfigMaps/Secrets are merged into the Quarkus configuration.

### Configuration

- Enable with `quarkus.kubernetes-config.enabled=true`.
- Set the namespace: `quarkus.kubernetes-config.namespace=my-namespace` (defaults to the pod's namespace).
- ConfigMap/Secret keys map directly to Quarkus config properties.

### Testing

- Disable in dev/test mode: `%dev.quarkus.kubernetes-config.enabled=false`.
- Use `application.properties` for dev/test and ConfigMaps for production.

### Common Pitfalls

- The application must have RBAC permissions to read ConfigMaps/Secrets in Kubernetes.
- Do NOT enable this in dev mode unless running inside a Kubernetes cluster — it will fail to connect.
