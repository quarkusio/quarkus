### Bean Scopes

- `@ApplicationScoped` — one instance per application (most common for services).
- `@RequestScoped` — one instance per HTTP request.
- `@Singleton` — like `@ApplicationScoped` but NOT proxied. Use only when proxy overhead matters.
- `@Dependent` — new instance per injection point. Avoid unless specifically needed.

### Dependency Injection

- Use `@Inject` on fields (package-private preferred) or constructor parameters.
- Constructor injection works without `@Inject` if there's only one constructor.
- Use `@Produces` methods to create beans that need custom initialization.

### Events

- Fire events with `Event<T>` injection and `event.fire(payload)`.
- Observe with `void onEvent(@Observes MyEvent event)`.
- Use `@ObservesAsync` for async event handling.

### Lifecycle and Startup

- `@Startup` on a bean class forces eager initialization at startup.
- Use `@PostConstruct` and `@PreDestroy` for lifecycle hooks.

### Interceptors

- Define interceptor binding with `@InterceptorBinding`.
- Implement interceptor with `@Interceptor` and `@AroundInvoke`.
- Register with `@Priority(value)`.

### Build-Time Discovery

- ArC discovers beans at build time (not runtime). Unscoped classes are NOT beans.
- Add a scope annotation to make a class a CDI bean.
- Use `@Unremovable` if a bean is only used via `Instance<T>` or programmatic lookup.

### Testing

- Use `@InjectMock` to replace beans in `@QuarkusTest`.
- Use `@QuarkusComponentTest` for lightweight CDI-only tests (no full app startup).

### Common Pitfalls

- Do NOT use `new MyService()` — always let CDI inject beans.
- `@Singleton` beans are NOT proxied — calling methods on `this` bypasses interceptors.
- Beans must have a no-arg constructor (or `@Inject` constructor) for CDI to manage them.
