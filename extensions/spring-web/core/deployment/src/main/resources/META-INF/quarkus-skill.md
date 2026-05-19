
### Controller Pattern

```java
@RestController
@RequestMapping("/api/items")
public class ItemController {

    @GetMapping
    public List<Item> listAll() { ... }

    @GetMapping("/{id}")
    public ResponseEntity<Item> getById(@PathVariable Long id) {
        return items.containsKey(id)
            ? ResponseEntity.ok(items.get(id))
            : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Item> create(@RequestBody Item item) {
        items.put(item.getId(), item);
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        items.remove(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public List<Item> search(@RequestParam String name) {
        return items.values().stream()
            .filter(i -> i.getName().contains(name))
            .toList();
    }
}
```

### Supported Annotations

| Annotation | Status |
|-----------|--------|
| `@RestController` | Supported |
| `@RequestMapping` | Supported (class-level path prefix) |
| `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@PatchMapping` | Supported |
| `@PathVariable` | Supported |
| `@RequestParam` | Supported |
| `@RequestBody` | Supported |
| `@RequestHeader` | Supported |
| `@CookieValue` | Supported |
| `ResponseEntity<T>` | Supported (for status codes and headers) |
| `@ResponseStatus` | Supported (on exceptions) |
| `@RestControllerAdvice` + `@ExceptionHandler` | Supported (single class only) |

### Exception Handling

Use `@ResponseStatus` on custom exceptions:

```java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ItemNotFoundException extends RuntimeException {
    public ItemNotFoundException(String message) { super(message); }
}
```

Or use `@RestControllerAdvice` for global exception handling:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ItemNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ItemNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getMessage()));
    }
}
```

Only **one** `@RestControllerAdvice` class is allowed per application.

### Testing

```java
@QuarkusTest
class ItemControllerTest {

    @Test
    void testCreate() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"id\":1,\"name\":\"Widget\"}")
            .when().post("/api/items")
            .then().statusCode(201);
    }

    @Test
    void testNotFound() {
        given()
            .when().get("/api/items/999")
            .then().statusCode(404);
    }
}
```

### Common Pitfalls

- **`params` attribute on mapping annotations is NOT supported**: `@GetMapping(params = "name")` causes a deployment error (duplicate endpoint). Use a different path like `@GetMapping("/search")` with `@RequestParam` instead.
- **Only one `@RestControllerAdvice`**: The application fails to start if multiple `@ControllerAdvice` classes exist.
- **`@ExceptionHandler` methods must be public instance methods** with return types: `void`, `ResponseEntity`, or a POJO.
- **No `ModelAndView`/`Model`/`View` support**: These MVC server-side rendering types are not supported. Use Qute templates instead.
- **No `@RequestMapping` on methods**: Use the specific `@GetMapping`/`@PostMapping` etc. instead of `@RequestMapping(method = ...)` on methods.
- **This is a compatibility layer**: For new code, consider using Quarkus REST (`@Path`, `@GET`, etc.) directly. The Spring Web extension is primarily for migrating existing Spring code.
