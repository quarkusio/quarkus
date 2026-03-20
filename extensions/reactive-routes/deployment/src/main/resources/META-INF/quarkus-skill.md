### Route Definition

- Annotate methods with `@Route(path = "/hello", methods = Route.HttpMethod.GET)`.
- Methods receive `RoutingContext` for full request/response control.
- Or use typed parameters: `@Route void hello(@Param String name, HttpServerResponse response)`.

### Route Ordering

- Use `@Route(order = 1)` to control execution order.
- Lower order values execute first.

### Testing

- Use `@QuarkusTest` with REST Assured — routes are accessible like REST endpoints.

### Common Pitfalls

- Reactive routes run on the Vert.x event loop — do NOT block.
- Use `@Blocking` on the method to offload to a worker thread.
