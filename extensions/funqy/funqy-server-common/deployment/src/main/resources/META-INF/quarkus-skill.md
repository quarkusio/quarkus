
### Function Definition

Write a plain CDI bean method annotated with `@Funq`:

```java
@ApplicationScoped
public class GreetingFunction {

    @Funq
    public String greet(String name) {
        return "Hello, " + name;
    }

    @Funq("custom-name")
    public Output process(Input input) {
        return new Output(input.getValue().toUpperCase());
    }
}
```

`@Funq` is from `io.quarkus.funqy.Funq`. The function name defaults to the method name; use `@Funq("name")` to customize.

### Platform Bindings

Choose one binding extension depending on where the function runs:

| Extension | Platform | Trigger |
|-----------|----------|---------|
| `funqy-http` | Standalone HTTP | HTTP POST to `/functionName` |
| `funqy-amazon-lambda` | AWS Lambda | Lambda event |
| `funqy-google-cloud-functions` | Google Cloud Functions | HTTP/CloudEvent |
| `funqy-knative-events` | Knative Eventing | CloudEvents |

### HTTP Binding

With `funqy-http`, each `@Funq` method becomes an HTTP endpoint:

```bash
# POST with JSON body
curl -X POST http://localhost:8080/greet \
  -H "Content-Type: application/json" \
  -d '"World"'
# Returns: "Hello, World"
```

The function name is the path. Input is the JSON request body. Output is the JSON response.

### Input/Output Types

- **No input**: method takes no parameters → triggered with empty body or GET
- **Primitive/String input**: JSON primitive in body
- **POJO input**: JSON object deserialized to the POJO
- **No output (void)**: returns 204 No Content
- **POJO output**: serialized to JSON
- **`Uni<T>` output**: async response

### Testing

```java
@QuarkusTest
class GreetingFunctionTest {
    @Test
    void testGreet() {
        given()
            .contentType(ContentType.JSON)
            .body("\"World\"")
            .when().post("/greet")
            .then()
            .statusCode(200)
            .body(equalTo("\"Hello, World\""));
    }
}
```

### Common Pitfalls

- **Only one parameter allowed**: `@Funq` methods take 0 or 1 parameter. Multiple parameters are not supported.
- **String input must be JSON-quoted**: Sending `World` fails — send `"World"` (with quotes) as valid JSON.
- **Don't mix with JAX-RS**: Funqy HTTP binding replaces JAX-RS for function endpoints. Don't annotate the same class with both `@Path` and `@Funq`.
- **Choose exactly one binding**: Adding multiple binding extensions (e.g., `funqy-http` + `funqy-amazon-lambda`) causes conflicts.
