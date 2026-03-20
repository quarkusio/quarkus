### Client Injection

- Inject `KubernetesClient` for Kubernetes API access.
- Use `client.pods().inNamespace("ns").list()` for resource operations.
- CRUD: `client.resources(MyCustomResource.class).create(resource)`.

### Watching Resources

- Use `client.pods().watch(new Watcher<Pod>() { ... })` for event watching.

### Dev Services

- Use `@WithKubernetesTestServer` for tests with a mock Kubernetes API.

### Testing

- Use `@QuarkusTest` with `@WithKubernetesTestServer` for mock K8s API.

### Common Pitfalls

- Ensure RBAC permissions are configured when running in-cluster.
- Close watchers to avoid resource leaks.
