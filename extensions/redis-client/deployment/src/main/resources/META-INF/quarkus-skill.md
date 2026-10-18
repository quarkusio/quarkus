
### Injecting the Data Source

```java
@Inject RedisDataSource redis;          // blocking (imperative)
@Inject ReactiveRedisDataSource reactive; // reactive (Mutiny-based)
```

### Command Groups

Access Redis commands through typed groups on `RedisDataSource`:

```java
HashCommands<String, String, String> hash = redis.hash(String.class);
ValueCommands<String, String> values = redis.value(String.class);
KeyCommands<String> keys = redis.key();
SortedSetCommands<String, String> sortedSets = redis.sortedSet(String.class);
ListCommands<String, String> lists = redis.list(String.class);
SetCommands<String, String> sets = redis.set(String.class);
```

The type parameter is the value type. Key type defaults to `String`. For non-string keys, use the full form: `redis.hash(UUID.class, String.class, Person.class)`.

### Hash Operations

```java
// Set fields
hash.hset("user:1", "name", "Alice");
hash.hset("user:1", Map.of("name", "Alice", "role", "admin"));

// Get fields
String name = hash.hget("user:1", "name");
Map<String, String> all = hash.hgetall("user:1");

// Delete fields
hash.hdel("user:1", "role");
```

### Value (String) Operations

```java
ValueCommands<String, String> values = redis.value(String.class);
values.set("greeting", "hello");
String v = values.get("greeting");

// Counters
ValueCommands<String, Long> counters = redis.value(Long.class);
counters.incr("counter");           // increment by 1
counters.incrby("counter", 5);      // increment by N
Long count = counters.get("counter"); // may be null if key doesn't exist
```

### Key Operations

```java
KeyCommands<String> keys = redis.key();
keys.expire("session:abc", Duration.ofMinutes(30));  // set TTL
keys.del("session:abc");                              // delete
boolean exists = keys.exists("session:abc");
List<String> matching = keys.keys("session:*");       // find by pattern
```

### Sorted Set Operations (Leaderboards)

```java
SortedSetCommands<String, String> zset = redis.sortedSet(String.class);
zset.zadd("leaderboard", 100.0, "player1");           // zadd(key, score, member)
zset.zadd("leaderboard", 250.0, "player2");
List<String> top = zset.zrange("leaderboard", 0, 9);  // ascending
// Descending (for leaderboards): use ZRangeArgs
List<ScoredValue<String>> topWithScores = zset.zrangeWithScores("leaderboard", 0, 9, new ZRangeArgs().rev());
OptionalDouble score = zset.zscore("leaderboard", "player1");  // returns OptionalDouble
OptionalLong rank = zset.zrevrank("leaderboard", "player1");   // returns OptionalLong, 0-based
```

Note: `zadd` takes **score first, then member**. `zscore`/`zrank`/`zrevrank` return `Optional` types.

### List Operations

```java
ListCommands<String, String> list = redis.list(String.class);
list.lpush("activity", "event1", "event2");     // push to head
List<String> recent = list.lrange("activity", 0, 19);  // get range
list.ltrim("activity", 0, 99);                  // keep only last 100
```

### Pub/Sub

```java
PubSubCommands<String> pubsub = redis.pubsub(String.class);
pubsub.publish("channel", "message");

// Subscribe (returns a subscriber that can be cancelled)
PubSubCommands.RedisSubscriber subscriber = pubsub.subscribe("channel", message -> {
    System.out.println("Received: " + message);
});
// Later: subscriber.unsubscribe();
```

### Reactive API

All command groups have reactive equivalents returning `Uni<T>` or `Multi<T>`:

```java
ReactiveHashCommands<String, String, String> hash = reactive.hash(String.class);
Uni<Map<String, String>> data = hash.hgetall("user:1");
```

### Dev Services

Redis Dev Service starts automatically in dev and test mode — no configuration needed.

### Testing

- Dev Services provides a real Redis in tests — no mocking needed.
- Clear keys in `@BeforeEach` for test isolation: `redis.flushall()`.

### Common Pitfalls

- `value.get()` returns `null` for non-existent keys — handle null when using counters.
- Command group instances are lightweight — create them per method call or cache in constructor.
- Use `key().keys("pattern*")` sparingly in production — it scans all keys. Use `SCAN` for large datasets.
- The blocking `RedisDataSource` must not be used on the event loop — use `@Blocking` on REST endpoints or use `ReactiveRedisDataSource`.
