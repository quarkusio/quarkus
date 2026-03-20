### WebSocket Endpoints

- Annotate a class with `@WebSocket(path = "/chat/{room}")`.
- Use `@OnOpen`, `@OnClose`, `@OnTextMessage`, `@OnBinaryMessage` on methods.
- Path parameters are injected as method parameters.

### Sending Messages

- Return a `String` or object from `@OnTextMessage` to send a response.
- Inject `WebSocketConnection` for programmatic sending.
- Use `connection.broadcast().sendText(msg)` for broadcasting.

### Testing

- Use `@QuarkusTest` with a WebSocket client (e.g. Vert.x `WebSocketClient`).

### Common Pitfalls

- `@WebSocket` (this extension) is NOT the same as `@ServerEndpoint` (old JSR 356 API).
- Do NOT use the old `quarkus-websockets` extension for new projects — use this one.
