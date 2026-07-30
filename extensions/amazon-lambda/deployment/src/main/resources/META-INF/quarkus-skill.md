
### Lambda Handler

Implement `RequestHandler<Input, Output>` from the AWS Lambda SDK as a CDI bean:

```java
@Named("greeting")
@ApplicationScoped
public class GreetingLambda implements RequestHandler<GreetingRequest, GreetingResponse> {

    @Override
    public GreetingResponse handleRequest(GreetingRequest input, Context context) {
        return new GreetingResponse("Hello, " + input.getName());
    }
}
```

If there's only one `RequestHandler` implementation, `@Named` is optional. With multiple handlers, use `@Named` and set `quarkus.lambda.handler` to select the active one.

### Stream Handler

For raw input/output streams, implement `RequestStreamHandler`:

```java
@Named("stream")
@ApplicationScoped
public class StreamLambda implements RequestStreamHandler {

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        // read input, write output
    }
}
```

### Configuration

```properties
# Select which handler to use (required when multiple exist)
quarkus.lambda.handler=greeting

# Expected exceptions (won't log stack traces)
quarkus.lambda.expected-exceptions=com.example.BusinessException
```

### Dev Mode Testing

In dev mode, a mock Lambda event server starts automatically on port 8080. Send test events via HTTP:

```bash
curl -X POST http://localhost:8080 \
  -H "Content-Type: application/json" \
  -d '{"name": "World"}'
```

### Testing

Use `@QuarkusTest` with the mock event server:

```java
@QuarkusTest
class GreetingLambdaTest {

    @Test
    void testGreeting() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"name\": \"World\"}")
            .when().post("/")
            .then()
            .statusCode(200)
            .body("greeting", equalTo("Hello, World"));
    }
}
```

The mock server runs on port 8081 during tests (`quarkus.lambda.test-port`).

### CDI Injection

Lambda handlers are CDI beans — use `@Inject` for any CDI bean:

```java
@ApplicationScoped
public class GreetingLambda implements RequestHandler<GreetingRequest, GreetingResponse> {

    @Inject GreetingService service;

    @Override
    public GreetingResponse handleRequest(GreetingRequest input, Context context) {
        return new GreetingResponse(service.greet(input.getName()));
    }
}
```

### Deployment

Build a native executable or uber-jar for Lambda deployment:

```bash
# Native (requires GraalVM or Mandrel)
./mvnw package -Pnative -Dquarkus.native.container-build=true

# JVM mode (uber-jar)
./mvnw package
```

The build generates `function.zip` in `target/` — ready for upload to AWS Lambda.

### Common Pitfalls

- **Multiple handlers require `@Named`**: If more than one `RequestHandler` exists, the build fails unless each has `@Named` and `quarkus.lambda.handler` is set.
- **Input/output must be serializable**: Request and response types are serialized via Jackson. Ensure they have default constructors and getters/setters.
- **Don't use JAX-RS with this extension**: `amazon-lambda` replaces the HTTP server entirely. For REST-based lambdas, use `amazon-lambda-rest` or `amazon-lambda-http` instead.
- **Mock server port**: Dev mode uses port 8080, test mode uses 8081. Override with `quarkus.lambda.dev-port` / `quarkus.lambda.test-port`.
