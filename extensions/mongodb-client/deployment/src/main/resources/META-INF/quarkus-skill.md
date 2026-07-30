
### Injecting the Client

```java
@Inject MongoClient mongoClient;          // blocking
@Inject ReactiveMongoClient reactiveClient; // reactive (Mutiny-based)
```

Choose **blocking** `MongoClient` for simple CRUD with `@Blocking` REST endpoints. Choose **reactive** `ReactiveMongoClient` when returning `Uni<T>` or `Multi<T>`.

### Getting a Database and Collection

```java
MongoDatabase db = mongoClient.getDatabase("mydb");
MongoCollection<Document> collection = db.getCollection("books");
```

Set the database name in config: `quarkus.mongodb.database=mydb`. Dev Services provides the connection string automatically but does NOT set the database name — you must configure it.

### CRUD Operations

```java
// Insert
Document doc = new Document("title", "Quarkus in Action").append("author", "Someone");
collection.insertOne(doc);

// Find all
List<Document> all = collection.find().into(new ArrayList<>());

// Find by ID
Document found = collection.find(Filters.eq("_id", new ObjectId(id))).first();

// Find with filter
List<Document> results = collection.find(Filters.eq("author", "Someone")).into(new ArrayList<>());

// Delete
collection.deleteOne(Filters.eq("_id", new ObjectId(id)));

// Update
collection.updateOne(Filters.eq("_id", new ObjectId(id)),
    Updates.set("title", "New Title"));
```

### ObjectId Handling

MongoDB's `ObjectId` does not serialize cleanly to JSON — it becomes an object, not a string. In DTOs/records, use `String` for the ID field and convert:

```java
// Document → DTO
String id = doc.getObjectId("_id").toHexString();

// DTO → filter
Filters.eq("_id", new ObjectId(stringId));
```

### POJO Codec (Typed Collections)

Quarkus auto-initializes the POJO codec — no manual `CodecRegistry` setup needed. Just request a typed collection:

```java
MongoCollection<Product> products = db.getCollection("products", Product.class);
products.insertOne(new Product("Widget", 9.99));
Product p = products.find(Filters.eq("name", "Widget")).first();
```

This works for both blocking and reactive clients. POJO classes need a no-arg constructor and public getters/setters (or be records with `@BsonCreator`).

### Reactive Client

```java
@Inject ReactiveMongoClient reactiveClient;

public Uni<List<Document>> listAll() {
    return reactiveClient.getDatabase("mydb")
        .getCollection("books")
        .find().collect().asList();
}
```

### Aggregation

```java
collection.aggregate(List.of(
    Aggregates.match(Filters.eq("category", "electronics")),
    Aggregates.group("$category", Accumulators.avg("avgPrice", "$price"))
)).into(new ArrayList<>());
```

### Dev Services

MongoDB Dev Service starts automatically in dev and test mode using a `mongo:7.0` container. The connection string is auto-configured. Set `quarkus.mongodb.database` in `application.properties` — this is NOT auto-configured by Dev Services.

### Multiple Named Clients

```properties
quarkus.mongodb.clients.inventory.connection-string=mongodb://...
quarkus.mongodb.clients.inventory.database=inventory
```

```java
@Inject @MongoClientName("inventory") MongoClient inventoryClient;
```

### Testing

- Dev Services provides a real MongoDB in tests — no mocking needed.
- For test isolation, clear collections in `@BeforeEach` or use unique collection names.

### Common Pitfalls

- `quarkus.mongodb.database` must be set explicitly — Dev Services only provides the connection string.
- `ObjectId` does not serialize to JSON as a string — use `toHexString()` in your DTOs.
- `find().into(new ArrayList<>())` collects results; `find().first()` returns a single document or null.
- The blocking `MongoClient` must not be used on the event loop thread — use `@Blocking` on REST endpoints or use `ReactiveMongoClient` instead.
