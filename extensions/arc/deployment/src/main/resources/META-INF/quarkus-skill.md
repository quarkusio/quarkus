### Bean Scopes

- `@ApplicationScoped` — one instance per application (most common for services).
- `@RequestScoped` — one instance per HTTP request.
- `@Singleton` — like `@ApplicationScoped` but NOT proxied. Use only when proxy overhead matters.
- `@Dependent` — new instance per injection point. Avoid unless specifically needed.

### Dependency Injection

- Use `@Inject` on fields (package-private preferred) or constructor parameters.
- `@Inject` is not required when there is exactly one constructor (Simplified Constructor Injection).
- `@Inject` is also not required when a qualifier annotation is used (e.g., `@Channel("my-channel") Emitter<String> emitter`).
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
- Lifecycle interceptors are also available: `@PostConstruct`, `@PreDestroy`, and `@AroundConstruct`.
- Register with `@Priority(value)`.

### Build-Time Discovery

- ArC discovers beans at build time (not runtime). Unscoped classes are NOT beans.
- Add a scope annotation to make a class a CDI bean. Note: some extensions add scope automatically (e.g. `@Scheduled`, `@WebSocket`).
- `Instance<T>` lookup is detected automatically. Use `@Unremovable` if a bean is only accessed via `CDI.current()` or ArC-specific APIs like `Arc.container()`.

### Testing

- Use `@InjectMock` to replace beans in `@QuarkusTest`.
- For `@Singleton` beans, use Mockito's `@InjectSpy` or configure the mock manually, since `@InjectMock` requires a client proxy (normal-scoped beans only).
- Use `@QuarkusComponentTest` for lightweight CDI-only tests (no full app startup).

### Common Pitfalls

- Do NOT use `new MyService()` — always let CDI inject beans.
- `@Singleton` beans are NOT proxied — there is no client proxy indirection, which gives slightly better performance.
- Quarkus supports Simplified Constructor Injection: a no-args constructor is generated automatically if missing, and `@Inject` is not required when there is exactly one constructor.
