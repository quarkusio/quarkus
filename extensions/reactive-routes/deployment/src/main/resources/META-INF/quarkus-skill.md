
### Declaring Routes

Use `@Route` on methods in a CDI bean. Group related routes with `@RouteBase`:

```java
@RouteBase(path = "/items", produces = "application/json")
@ApplicationScoped
public class ItemRoutes {

    @Route(methods = Route.HttpMethod.GET)
    public List<Item> list() {
        return items;
    }

    @Route(methods = Route.HttpMethod.POST)
    public Item create(@Body Item item) {
        items.add(item);
        return item;
    }

    @Route(methods = Route.HttpMethod.GET, path = "/:id")
    public Item get(@Param String id) {
        return findById(id);
    }

    @Route(methods = Route.HttpMethod.DELETE, path = "/:id")
    public void delete(@Param String id, HttpServerResponse response) {
        items.remove(id);
        response.setStatusCode(204).end();
    }
}
```

`HttpMethod` is `io.quarkus.vertx.web.Route.HttpMethod` — not `io.vertx.core.http.HttpMethod`.

### Void Methods

Methods returning `void` **must** accept at least one parameter that can end the response: `RoutingContext`, `RoutingExchange`, `HttpServerRequest`, `HttpServerResponse`, or their Mutiny variants. Non-void methods auto-serialize the return value.

### Parameter Injection

- `@Param String name` — path/query parameter (String, `Optional<String>`, or `List<String>`)
- `@Body Item item` — request body (auto-deserialized via Jackson for POJOs)
- `@Header("X-Custom") String h` — request header (String, `Optional<String>`, or `List<String>`)
- `RoutingContext rc` — full Vert.x routing context for manual control

### Failure Handlers

Accept a typed exception parameter to match specific exception types:

```java
@Route(type = Route.HandlerType.FAILURE)
public void handleNotFound(NotFoundException e, HttpServerResponse response) {
    response.setStatusCode(404)
        .putHeader("content-type", "application/json")
        .end("{\"error\":\"" + e.getMessage() + "\"}");
}
```

Omitting `path` and `regex` matches all routes. Use `order` to control precedence when multiple handlers match.

### Filters

```java
@RouteFilter(100)
public void log(RoutingContext rc) {
    // higher priority value = called first
    rc.next(); // MUST call next() to continue the chain
}
```

The method must return `void` and accept exactly one `RoutingContext` parameter.

### Streaming with Multi

Control serialization via `produces` on `@Route`:

```java
@Route(path = "/stream", produces = ReactiveRoutes.EVENT_STREAM)
public Multi<Item> sse() { ... } // text/event-stream (SSE)

@Route(path = "/stream", produces = ReactiveRoutes.ND_JSON)
public Multi<Item> ndjson() { ... } // newline-delimited JSON

@Route(path = "/stream", produces = ReactiveRoutes.APPLICATION_JSON)
public Multi<Item> jsonArray() { ... } // chunked JSON array
```

For custom SSE events, return `Multi<ReactiveRoutes.ServerSentEvent<T>>` and implement `event()`, `data()`, and optionally `id()`.

### Blocking Routes

```java
@Route(path = "/compute", type = Route.HandlerType.BLOCKING)
public Result heavyComputation() { ... }
```

### Path Derivation

If neither `path` nor `regex` is set, the path is derived from the method name by de-camel-casing and joining with hyphens (e.g., `getActiveUsers` → `/get-active-users`). With `@RouteBase(path = "/api")`, it becomes `/api/get-active-users`.

### Common Pitfalls

- **Wrong HttpMethod import**: Use `io.quarkus.vertx.web.Route.HttpMethod`, not `io.vertx.core.http.HttpMethod`.
- **Void methods without response-ending parameter**: Will fail at build time. Either return a value or accept `RoutingContext`/`HttpServerResponse`.
- **Forgetting `rc.next()` in filters**: The request will hang. Always call `rc.next()` to pass control to the next handler.
- **`@Param` type**: Must be `String`, `Optional<String>`, or `List<String>` — no automatic type conversion to Long/Integer.
- **Deprecated wrapper methods**: `ReactiveRoutes.asEventStream()`, `asJsonStream()`, `asJsonArray()` are deprecated. Use the `produces` attribute instead.
