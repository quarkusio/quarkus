
### Server Endpoints

- Annotate a class with `@WebSocket(path = "/my-endpoint/{param}")` — it becomes a CDI bean (default `@Singleton`).
- Use callback annotations: `@OnOpen`, `@OnTextMessage`, `@OnBinaryMessage`, `@OnClose`, `@OnError`.
- Callback method parameters: annotate with `@io.quarkus.websockets.next.PathParam` to receive path parameters, or accept `WebSocketConnection` or `HandshakeRequest` directly. See the [method parameters reference](https://quarkus.io/guides/websockets-next-reference#method-parameters).
- Inject `WebSocketConnection` to access `pathParam("param")`, `id()`, `broadcast()`, `getOpenConnections()`.
- Return a value from a callback to send it to the client; use `broadcast = true` on `@OnTextMessage` or `@OnOpen` to broadcast the return value to all connected clients instead.
- Use `@WebSocket(endpointId = "my-id")` to assign a stable endpoint ID for use with `OpenConnections`.

### Broadcasting

- `@OnTextMessage(broadcast = true)` — return value is sent to ALL open connections.
- `connection.broadcast().sendTextAndAwait(msg)` — programmatic broadcast from any callback.
- `connection.broadcast().filter(c -> !c.id().equals(excludeId)).sendTextAndAwait(msg)` — broadcast with filter.
- Cross-endpoint broadcasting — inject `OpenConnections` and iterate:
  ```java
  @Inject OpenConnections connections;
  for (var conn : connections.findByEndpointId("notifications")) {
      conn.sendTextAndAwait(message);
  }
  ```

### @OnClose Behavior

- `@OnClose` methods must return `void` or `Uni<Void>` — they cannot return a message.
- The closing connection is already closed when `@OnClose` fires — you cannot send to it.
- Broadcasting from `@OnClose` WORKS — it sends to all OTHER open connections.

### Serialization

- Objects are auto-serialized to/from JSON. `String`, `JsonObject`, `JsonArray`, `Buffer`, `byte[]` are sent as-is.
- For custom serialization, implement `TextMessageCodec<T>` as a CDI bean:
  ```java
  @Singleton
  public class MyCodec implements TextMessageCodec<MyType> {
      @Override
      public boolean supports(Type type) { return type.equals(MyType.class); }
      @Override
      public String encode(MyType value) { return JsonObject.mapFrom(value).encode(); }
      @Override
      public MyType decode(Type type, String value) { return new JsonObject(value).mapTo(MyType.class); }
  }
  ```
- Note: `supports()` and `decode()` take `java.lang.reflect.Type`, not `Class<?>`.

### Streams

- Accept `Multi<X>` as a parameter in `@OnTextMessage` for streaming input.
- Return `Multi<X>` from `@OnOpen` to establish a persistent server-push stream for the connection's lifetime.
- Return `Multi<X>` from `@OnTextMessage` for bidirectional streaming — each incoming message produces a stream of responses.
- When returning `void` with a `Multi` parameter, you MUST subscribe to the `Multi` manually.

### HTTP Upgrade Checks

- Implement `HttpUpgradeCheck` as a CDI bean to validate upgrade requests:
  ```java
  @Singleton
  public class TokenCheck implements HttpUpgradeCheck {
      @Override
      public Uni<CheckResult> perform(HttpUpgradeContext context) {
          String token = context.httpRequest().getParam("token");
          if (token == null) {
              return CheckResult.rejectUpgrade(403);
          }
          return CheckResult.permitUpgrade();
      }
  }
  ```
- `perform()` returns `Uni<CheckResult>` — use `CheckResult.permitUpgrade()` or `CheckResult.rejectUpgrade(statusCode)`.
- Access query params via `context.httpRequest().getParam("name")`, headers via `context.httpRequest().headers()`.
- Use `appliesTo(String endpointId)` to scope the check to specific endpoints.
- Beans must be `@ApplicationScoped`, `@Singleton`, or `@Dependent` — NOT `@RequestScoped`.

### Testing

- Use `BasicWebSocketConnector` or `WebSocketConnector` for test clients — they are the standard API and handle context propagation:
  ```java
  @Inject BasicWebSocketConnector connector;

  @Test
  void testEndpoint() throws Exception {
      CountDownLatch messageLatch = new CountDownLatch(1);
      List<String> received = new CopyOnWriteArrayList<>();

      var connection = connector
          .baseUri(uri)
          .path("/my-endpoint/myParam")
          .onTextMessage((c, msg) -> {
              received.add(msg);
              messageLatch.countDown();
          })
          .connectAndAwait();
      connection.sendTextAndAwait("hello");
      assertTrue(messageLatch.await(5, TimeUnit.SECONDS));
      connection.closeAndAwait();
  }
  ```
- Use `CountDownLatch` for synchronization — WebSocket messages are async.
- Messages are received as raw `String` on the client side — parse JSON manually with `new JsonObject(msg)`.

### Common Pitfalls

- `@OnClose` cannot return a message — use `connection.broadcast().sendTextAndAwait()` instead.
- `WebSocketConnection` is session-scoped; do NOT inject it in `@ApplicationScoped` beans outside endpoint classes — it won't resolve to the correct connection.
- `broadcast()` includes the sender connection — use `.filter()` to exclude it if needed.
- Blocking methods (returning `void`) run on a worker thread by default; use `@NonBlocking` for event loop execution.
