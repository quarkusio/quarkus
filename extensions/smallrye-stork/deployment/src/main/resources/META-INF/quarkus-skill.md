### Usage

- Add this extension for client-side service discovery and load balancing.
- Configure service discovery and load balancing per service with `quarkus.stork.<service-name>.*`.
- Integrates automatically with the REST Client — use `@RegisterRestClient(baseUri = "stork://my-service")`.

### Service Discovery

- Supported backends: Kubernetes, Consul, Eureka, DNS, static list.
- Example: `quarkus.stork.my-service.service-discovery.type=kubernetes`.

### Load Balancing

- Strategies: `round-robin` (default), `random`, `least-requests`, `power-of-two-choices`.
- Example: `quarkus.stork.my-service.load-balancer.type=round-robin`.

### Testing

- Use static service discovery in tests: `quarkus.stork.my-service.service-discovery.type=static`.
- Provide test addresses: `quarkus.stork.my-service.service-discovery.address-list=localhost:8081`.

### Common Pitfalls

- Stork is for CLIENT-SIDE discovery — it does not register your service; it discovers others.
- The `stork://` URI scheme only works with the REST Client integration.
