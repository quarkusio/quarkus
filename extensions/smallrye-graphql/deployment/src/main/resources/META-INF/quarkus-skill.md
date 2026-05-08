
### Creating a GraphQL API

Annotate a CDI bean with `@GraphQLApi`:

```java
@GraphQLApi
public class BookApi {

    @Query
    public List<Book> allBooks() { ... }

    @Query
    public Book book(@Name("id") Long id) { ... }

    @Mutation
    public Book createBook(Book book) { ... }

    @Mutation
    public boolean deleteBook(Long id) { ... }
}
```

- `@Query` methods become GraphQL queries. Method name = query name.
- `@Mutation` methods become GraphQL mutations.
- No configuration needed — endpoint is at `/graphql`, UI at `/q/graphql-ui`.

### Type Mapping

| Java Type | GraphQL Type |
|-----------|-------------|
| `String` | `String` |
| `int/Integer` | `Int` |
| `long/Long` | `BigInteger` |
| `float/double` | `Float` |
| `boolean` | `Boolean` |
| Enums | GraphQL enum |
| POJOs | GraphQL object type |

Note: Java `Long` maps to `BigInteger` in GraphQL, not `Int`. In JSON responses, IDs of type `Long` come back as integers — extract them as `Integer` in tests, not `String`.

### Nested Types with @Source

Resolve related types lazily using `@Source`:

```java
@GraphQLApi
public class BookApi {

    @Query
    public Book book(Long id) { ... }

    public Author author(@Source Book book) {
        return authorService.findById(book.getAuthorId());
    }

    public List<Review> reviews(@Source Book book) {
        return reviewService.findByBookId(book.getId());
    }
}
```

The `@Source` methods are called only when the client requests those fields.

### Type Naming

GraphQL type names are derived from Java class names:

- **Output types**: class name is used directly — `Book` → `type Book`.
- **Input types**: `Input` suffix is appended automatically — `Book` used as a mutation parameter → `input BookInput`.
- **Generic types**: type parameter is appended with `_` — `Page<Book>` → `Page_Book`, `ResultWrapper<String>` → `ResultWrapper_String`.

Override names with `@Type("CustomName")` for output types, `@Input("CustomName")` for input types, or `@Name("CustomName")` for either.

### Input Types

Any POJO used as a mutation parameter automatically becomes a GraphQL input type. Use `@Input` on the class to control its input type name:

```java
@Input("CreateBookInput")
public class BookInput {
    public String title;
    public String isbn;
    public Long authorId;
}

@Mutation
public Book createBook(BookInput book) { ... }
```

Without `@Input`, the class name + `Input` suffix is used as the GraphQL input type name (e.g., a class named `BookInput` becomes `BookInputInput` in the schema). Use `@Input` to set the name explicitly.

### Error Handling

Create custom exception classes with `@ErrorCode` (from `io.smallrye.graphql.api`):

```java
@ErrorCode("NOT_FOUND")
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) { super(message); }
}
```

Then throw it from your API: `throw new NotFoundException("Book not found");`

Configure error visibility in `application.properties`:
```properties
quarkus.smallrye-graphql.show-runtime-exception-message=com.example.NotFoundException
quarkus.smallrye-graphql.error-extension-fields=exception,classification,code,description
```

Without `show-runtime-exception-message`, error messages show as "System error" instead of your custom message.

### Schema Customization

- `@Name("CustomName")` — rename a type (input or output), field, or parameter in the schema.
- `@Type("CustomName")` — rename an output type in the schema (class-level only).
- `@Input("CustomName")` — rename an input type in the schema (class-level only).
- `@Description("A book in the catalog")` — add description to a type or field.
- `@NonNull` — mark a field as non-nullable in the schema.
- `@DefaultValue("10")` — set a default value for a query argument.

### Pagination Pattern

```java
@Query
public List<Book> books(@DefaultValue("0") int page, @DefaultValue("10") int size) {
    return bookService.findAll().stream()
        .skip((long) page * size)
        .limit(size)
        .toList();
}
```

### Testing

Send GraphQL queries as POST requests to `/graphql`:

```java
String query = """
    { "query": "{ allBooks { id title author { name } } }" }
    """;

given()
    .contentType(ContentType.JSON)
    .body(query)
    .post("/graphql")
    .then()
    .statusCode(200)
    .body("data.allBooks.size()", is(3));
```

For mutations:
```java
String mutation = """
    { "query": "mutation { createBook(title: \\"My Book\\") { id title } }" }
    """;
```

### Dev MCP Tools

Use `quarkus-smallrye-graphql_getGraphQLSchema` to inspect the generated schema at runtime.

### Common Pitfalls

- Java `Long` → GraphQL `BigInteger`: in test JSON responses, IDs are integers, not strings.
- `@Query` and `@Mutation` methods must be on a `@GraphQLApi` CDI bean — plain classes are ignored.
- The `"query"` field in the POST body is used for both queries AND mutations.
- Test isolation: GraphQL APIs sharing in-memory state need `@BeforeEach` cleanup.
