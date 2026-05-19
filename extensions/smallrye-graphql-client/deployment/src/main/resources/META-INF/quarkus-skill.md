
### Typesafe Client

Define an interface that mirrors the GraphQL server operations:

```java
@GraphQLClientApi(configKey = "book-client")
public interface BookClient {

    @Query
    List<Book> books();

    @Mutation
    Book addBook(String title, String author);
}
```

Annotations: `@Query`, `@Mutation` from `org.eclipse.microprofile.graphql`. The interface is from `io.smallrye.graphql.client.typesafe.api.GraphQLClientApi`.

Inject like any CDI bean:

```java
@Inject BookClient bookClient;
```

Configure the target URL:

```properties
quarkus.smallrye-graphql-client.book-client.url=http://localhost:8080/graphql
```

The config key can be the `configKey` annotation parameter, the class simple name, or the fully qualified class name.

### Dynamic Client

For queries built at runtime, inject `DynamicGraphQLClient` with `@GraphQLClient`:

```java
@Inject
@GraphQLClient("my-client")
DynamicGraphQLClient client;
```

Build queries programmatically:

```java
import static io.smallrye.graphql.client.core.Document.document;
import static io.smallrye.graphql.client.core.Field.field;
import static io.smallrye.graphql.client.core.Argument.arg;

Document query = document(
    Operation.operation(field("books", arg("author", "Tolkien"),
        field("title"), field("author"))));

Response response = client.executeSync(query);
List<Book> books = response.getList(Book.class, "books");
```

Configure the same way:

```properties
quarkus.smallrye-graphql-client.my-client.url=http://host/graphql
```

### Custom Headers

```properties
quarkus.smallrye-graphql-client.my-client.header.Authorization=Bearer my-token
quarkus.smallrye-graphql-client.my-client.header.X-Custom=value
```

### Model Classes

Model classes are shared between client and server — no separate DTOs needed. Use public fields or getters/setters. The GraphQL client serializes/deserializes using the same model.

### Testing

Inject the client directly in `@QuarkusTest`:

```java
@QuarkusTest
class BookClientTest {
    @Inject BookClient client;

    @Test
    void queryBooks() {
        List<Book> books = client.books();
        assertFalse(books.isEmpty());
    }
}
```

When testing against a local GraphQL server in the same app, point the client URL to the test port:

```properties
%test.quarkus.smallrye-graphql-client.book-client.url=http://localhost:8081/graphql
```

### Common Pitfalls

- **Client method names must match GraphQL operation names.** If the server defines `@Query("allBooks")`, the client method must be named `allBooks()` or use `@Query("allBooks")`.
- **Default GraphQL endpoint is `/graphql`** — don't forget this suffix in the URL configuration.
- **Config key matching**: The `configKey` in `@GraphQLClientApi` must match the key in `quarkus.smallrye-graphql-client.<configKey>.url`.
- **WebSocket subprotocol**: Defaults to `graphql-transport-ws` (the newer protocol). For older Apollo servers, set `subprotocols=graphql-ws`.
- **Test isolation**: Application state persists across `@QuarkusTest` methods — mutations in one test affect subsequent queries.
