### Usage

- Add this extension for Jakarta Servlet support in Quarkus.
- Use `@WebServlet`, `@WebFilter`, and `@WebListener` annotations.
- Useful for migrating legacy servlet-based applications to Quarkus.

### Pattern

```java
@WebServlet(urlPatterns = "/legacy/*")
public class MyServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.getWriter().write("Hello from Servlet");
    }
}
```

### Testing

- Use `@QuarkusTest` with REST Assured to test servlet endpoints.

### Common Pitfalls

- For new applications, prefer `quarkus-rest` (Jakarta REST) over servlets — it is more efficient and idiomatic in Quarkus.
- Servlet filters and REST filters are separate stacks — mixing them can cause confusion.
- Not all servlet features are supported in native image — check the guide for limitations.
