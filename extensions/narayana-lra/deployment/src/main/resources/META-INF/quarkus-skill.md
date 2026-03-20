### Usage

- Add this extension for Long Running Actions (LRA) — a saga pattern for distributed transactions.
- Annotate methods with `@LRA` to start/join a long running action.
- Use `@Compensate` and `@Complete` to define compensation and completion callbacks.

### Pattern

```java
@Path("/orders")
public class OrderResource {
    @POST
    @LRA(LRA.Type.REQUIRES_NEW)
    public Response createOrder(Order order) { ... }

    @PUT
    @Path("/compensate")
    @Compensate
    public Response compensate(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId) { ... }

    @PUT
    @Path("/complete")
    @Complete
    public Response complete(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId) { ... }
}
```

### Testing

- Use `@QuarkusTest` — an LRA coordinator is needed (Narayana LRA coordinator).
- Test both the success path (complete) and failure path (compensate).

### Common Pitfalls

- An LRA coordinator must be running — configure with `quarkus.lra.coordinator-url`.
- `@Compensate` and `@Complete` methods must be idempotent — they may be called multiple times.
