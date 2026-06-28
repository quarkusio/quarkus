
### Usage

SmallRye Stork provides service discovery and load balancing for client-side service lookup. It integrates with REST Client and gRPC client.

### Service Discovery with REST Client

Configure Stork to discover services, then use `stork://` URLs in REST Client:

```java
@RegisterRestClient(baseUri = "stork://my-service")
public interface MyServiceClient {
    @GET @Path("/api/data")
    String getData();
}
```

### Configuration

```properties
# Static list of service instances
quarkus.stork.my-service.service-discovery.type=static
quarkus.stork.my-service.service-discovery.address-list=localhost:8081,localhost:8082

# Consul service discovery
quarkus.stork.my-service.service-discovery.type=consul
quarkus.stork.my-service.service-discovery.consul-host=localhost
quarkus.stork.my-service.service-discovery.consul-port=8500

# Kubernetes service discovery
quarkus.stork.my-service.service-discovery.type=kubernetes
quarkus.stork.my-service.service-discovery.k8s-namespace=production
```

### Load Balancing

```properties
# Round-robin (default)
quarkus.stork.my-service.load-balancer.type=round-robin

# Random
quarkus.stork.my-service.load-balancer.type=random

# Least requests
quarkus.stork.my-service.load-balancer.type=least-requests

# Least response time
quarkus.stork.my-service.load-balancer.type=least-response-time
```

### Programmatic Access

```java
@Inject @RestClient MyServiceClient client; // uses Stork transparently

// Or access Stork directly
@Inject Stork stork;

ServiceInstance instance = stork.getService("my-service")
    .selectInstance()
    .await().indefinitely();
String host = instance.getHost();
int port = instance.getPort();
```

### Common Pitfalls

- **Requires a service discovery provider**: The `stork` extension alone is not enough. Add the appropriate discovery extension (e.g., `stork-consul`, `stork-kubernetes`) or use `static` type.
- **`stork://` URL scheme**: REST Client must use `stork://service-name` as the base URI — not `http://`. The scheme triggers Stork resolution.
- **Service name matching**: The service name in `stork://my-service` must match the config key in `quarkus.stork.my-service.*`.
- **Static is for dev/test**: `static` address list is useful for development. Switch to `consul` or `kubernetes` for production.
- **Load balancer is optional**: If not configured, `round-robin` is used by default.
