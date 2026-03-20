### Usage

- Add this extension to write Google Cloud Functions with direct access to the Google Cloud Functions SDK.
- Implement `HttpFunction`, `BackgroundFunction<T>`, or `RawBackgroundFunction` interfaces.
- For simple HTTP REST endpoints, prefer `quarkus-google-cloud-functions-http` instead.

### Pattern

```java
public class HelloFunction implements HttpFunction {
    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        response.getWriter().write("Hello from Quarkus!");
    }
}
```

### Testing

- Use `@QuarkusTest` for integration testing.

### Common Pitfalls

- For REST endpoints, `quarkus-google-cloud-functions-http` is simpler — it reuses your existing REST resources.
- For event-driven functions, prefer `quarkus-funqy-google-cloud-functions` for a simpler programming model.
