
### Injecting the Client

```java
@Inject KubernetesClient client;
```

The client is from the Fabric8 Kubernetes Client library (`io.fabric8.kubernetes.client.KubernetesClient`).

### Common Operations

```java
// List pods in a namespace
PodList pods = client.pods().inNamespace("default").list();

// Get a specific resource
Pod pod = client.pods().inNamespace("default").withName("my-pod").get();

// Create a resource
client.pods().inNamespace("default").resource(pod).create();

// Delete a resource
client.pods().inNamespace("default").withName("my-pod").delete();

// Watch for changes
client.pods().inNamespace("default").watch(new Watcher<Pod>() {
    @Override
    public void eventReceived(Action action, Pod pod) {
        // handle event
    }
    @Override
    public void onClose(WatcherException e) { }
});
```

### Custom Resources (CRDs)

```java
// Define a custom resource POJO
@Group("example.com")
@Version("v1")
public class MyResource extends CustomResource<MyResourceSpec, MyResourceStatus>
    implements Namespaced { }

// Use typed client
MixedOperation<MyResource, KubernetesResourceList<MyResource>, Resource<MyResource>> crClient =
    client.resources(MyResource.class);

crClient.inNamespace("default").list();
```

### Configuration

```properties
# Usually auto-detected from cluster environment or ~/.kube/config
# Override explicitly:
quarkus.kubernetes-client.api-server-url=https://kubernetes.default.svc
quarkus.kubernetes-client.namespace=my-namespace
quarkus.kubernetes-client.trust-certs=true
```

In dev mode, the client uses your local `~/.kube/config` automatically.

### Testing

The recommended approach is to use the Kubernetes Dev Service, which starts an API server (k3s) automatically in dev and test mode — no local cluster or mock needed:

```java
@QuarkusTest
class KubernetesTest {
    @Inject KubernetesClient client;

    @Test
    void testListPods() {
        PodList pods = client.pods().list();
        assertNotNull(pods);
    }
}
```

Alternatively, `@WithKubernetesTestServer` provides a mock API server (add `quarkus-test-kubernetes-client` test dependency).

### Common Pitfalls

- **Auto-configures from kubeconfig**: In dev mode, the client reads `~/.kube/config`. Ensure your local cluster context is correct.
- **Namespace is important**: Operations without `.inNamespace()` use the default namespace. Be explicit.
- **Client is closeable**: The injected `KubernetesClient` is managed by CDI — don't close it manually.
- **RBAC permissions**: The client's operations are limited by the service account's RBAC permissions in the cluster. Errors from missing permissions show as `403 Forbidden`.
