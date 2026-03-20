### Usage

- Add this extension to expose Funqy functions over HTTP.
- Annotate methods with `@Funq` — they are automatically exposed as HTTP endpoints.
- Funqy provides a cloud-agnostic function programming model.

### Pattern

```java
public class GreetingFunction {
    @Funq
    public String greet(String name) {
        return "Hello " + name;
    }
}
```

### Testing

- Use `@QuarkusTest` with REST Assured to test functions via HTTP.
- Functions are exposed at `/<functionName>` by default.

### Common Pitfalls

- Funqy functions take at most one input parameter and return one output.
- For full REST API control (path params, headers, status codes), use `quarkus-rest` instead.
