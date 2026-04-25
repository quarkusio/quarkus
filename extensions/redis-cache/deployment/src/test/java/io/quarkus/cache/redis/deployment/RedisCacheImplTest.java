package io.quarkus.cache.redis.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.GenericContainer;

import io.quarkus.cache.redis.runtime.RedisCacheImpl;
import io.quarkus.cache.redis.runtime.RedisCacheInfo;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.json.Json;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.Request;
import io.vertx.mutiny.redis.client.Response;
import io.vertx.redis.client.RedisOptions;

class RedisCacheImplTest {

    private static final Supplier<Boolean> BLOCKING_ALLOWED = () -> false;

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest();

    @Inject
    Vertx vertx;

    @Inject
    Redis redis;

    @AfterEach
    void clear() {
        try {
            redis.send(Request.cmd(Command.FLUSHALL).arg("SYNC")).await()
                    .atMost(Duration.ofSeconds(10));
        } catch (Exception ignored) {
            // ignored.
        }
    }

    @Test
    public void testComputeActualKey() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "foo";
        info.prefix = null;
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(2));

        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);
        assertThat(cache.computeActualKey("keyname")).isEqualTo("cache:foo:keyname");
    }

    @Test
    public void testComputeActualKeyWithCustomPrefix() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "foo";
        info.prefix = "my-prefix";
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(2));

        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);
        assertThat(cache.computeActualKey("keyname")).isEqualTo("my-prefix:keyname");
    }

    @Test
    public void testComputeActualKeyWithCustomPrefixUsingCacheNameVariable() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "foo";
        info.prefix = "my-prefix:{cache-name}";
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(2));

        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);
        assertThat(cache.computeActualKey("keyname")).isEqualTo("my-prefix:foo:keyname");
    }

    @Test
    public void testPutInTheCache() {
        String k = UUID.randomUUID().toString();
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "foo";
        info.valueType = String.class;
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(2));
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);
        assertThat(cache.get(k, s -> "hello").await().indefinitely()).isEqualTo("hello");
        var r = redis.send(Request.cmd(Command.GET).arg("cache:foo:" + k)).await().indefinitely();
        assertThat(r).isNotNull();
    }

    @Test
    public void testExhaustConnectionPool() {
        String redisUrl = ConfigProvider.getConfig().getValue("quarkus.redis.hosts", String.class);

        String k = UUID.randomUUID().toString();
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "foo";
        info.valueType = String.class;
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(2));

        Redis redis = Redis.createClient(vertx, new RedisOptions()
                .setMaxPoolSize(1)
                .setMaxPoolWaiting(0)
                .setConnectionString(redisUrl));

        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        List<Uni<String>> responses = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            responses.add(cache.get(k, s -> "hello"));
        }

        final var values = Uni.combine().all().unis(responses).with(list -> list).await().indefinitely();
        assertThat(values).isNotEmpty().allMatch(value -> value.equals("hello"));

        var r = redis.send(Request.cmd(Command.GET).arg("cache:foo:" + k)).await().indefinitely();
        assertThat(r).isNotNull();

        redis.close();
    }

    @Test
    public void testPutInTheCacheWithoutRedis() {
        // must start our own Redis server, because we need to stop it in the middle of the test
        GenericContainer<?> server = new GenericContainer<>("redis:7").withExposedPorts(6379);
        server.start();
        Redis redis = Redis.createClient(vertx, "redis://" + server.getHost() + ":" + server.getFirstMappedPort());

        String k = UUID.randomUUID().toString();
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "foo";
        info.valueType = String.class;
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(2));
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);
        server.close();
        assertThat(cache.get(k, s -> "hello").await().indefinitely()).isEqualTo("hello");
        redis.close();
    }

    @Test
    public void testPutInTheCacheWithOptimisticLocking() {
        String k = UUID.randomUUID().toString();
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "foo";
        info.valueType = String.class;
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(2));
        info.useOptimisticLocking = true;
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);
        assertThat(cache.get(k, s -> "hello").await().indefinitely()).isEqualTo("hello");
        var r = redis.send(Request.cmd(Command.GET).arg("cache:foo:" + k)).await().indefinitely();
        assertThat(r).isNotNull();
    }

    @Test
    public void testPutAndWaitForInvalidation() {
        String k = UUID.randomUUID().toString();
        RedisCacheInfo info = new RedisCacheInfo();
        info.valueType = String.class;
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(1));
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);
        assertThat(cache.get(k, s -> "hello").await().indefinitely()).isEqualTo("hello");
        var x = cache.get(k, String::toUpperCase).await().indefinitely();
        assertEquals(x, "hello");
        await().until(() -> cache.getOrNull(k, String.class).await().indefinitely() == null);
    }

    @Test
    public void testExpireAfterReadAndWrite() throws InterruptedException {
        String k = UUID.randomUUID().toString();
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "foo";
        info.valueType = String.class;
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(1));
        info.expireAfterAccess = Optional.of(Duration.ofSeconds(1));
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);
        assertThat(cache.get(k, s -> "hello").await().indefinitely()).isEqualTo("hello");
        var x = cache.get(k, String::toUpperCase).await().indefinitely();
        assertEquals(x, "hello");
        for (int i = 0; i < 3; i++) {
            x = cache.get(k, String::toUpperCase).await().indefinitely();
            assertEquals(x, "hello");
            Thread.sleep(500);
        }
        await().until(() -> redis.send(Request.cmd(Command.GET).arg("cache:foo:" + k)).await().indefinitely() == null);
        await().until(() -> cache.getOrNull(k, String.class).await().indefinitely() == null);
    }

    @Test
    public void testManualInvalidation() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.valueType = String.class;
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(10));
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);
        cache.get("foo", s -> "hello").await().indefinitely();
        var x = cache.get("foo", String::toUpperCase).await().indefinitely();
        assertEquals(x, "hello");

        cache.invalidate("foo").await().indefinitely();
        String foo = cache.get("foo", String.class, String::toUpperCase).await().indefinitely();
        assertThat(foo).isEqualTo("FOO");
    }

    public static class Person {
        public String firstName;
        public String lastName;

        public Person(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public Person() {
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Person person = (Person) o;
            return firstName.equals(person.firstName) && lastName.equals(person.lastName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(firstName, lastName);
        }
    }

    @Test
    public void testGetOrNull() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(10));
        info.valueType = Person.class;
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);
        Person person = cache.getOrNull("foo", Person.class).await().indefinitely();
        assertThat(person).isNull();
        assertThatTheKeyDoesNotExist("cache:foo");

        cache.get("foo", Person.class, s -> new Person(s, s.toUpperCase())).await().indefinitely();
        person = cache.getOrNull("foo", Person.class).await().indefinitely();
        assertThat(person).isNotNull()
                .satisfies(p -> {
                    assertThat(p.firstName).isEqualTo("foo");
                    assertThat(p.lastName).isEqualTo("FOO");
                });
        assertThatTheKeyDoesExist("cache:default-redis-cache:foo");
    }

    @Test
    public void testGetOrDefault() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(10));
        info.valueType = Person.class;
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);
        Person person = cache.getOrDefault("foo", new Person("bar", "BAR")).await().indefinitely();
        assertThat(person).isNotNull()
                .satisfies(p -> {
                    assertThat(p.firstName).isEqualTo("bar");
                    assertThat(p.lastName).isEqualTo("BAR");
                });
        // Verify it was not stored
        person = cache.getOrNull("foo", Person.class).await().indefinitely();
        assertThat(person).isNull();

        cache.get("foo", Person.class, s -> new Person(s, s.toUpperCase())).await().indefinitely();
        person = cache.getOrNull("foo", Person.class).await().indefinitely();
        assertThat(person).isNotNull()
                .satisfies(p -> {
                    assertThat(p.firstName).isEqualTo("foo");
                    assertThat(p.lastName).isEqualTo("FOO");
                });
        assertThatTheKeyDoesExist("cache:default-redis-cache:foo");
    }

    @Test
    public void testCacheNullValue() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(10));
        info.valueType = Person.class;
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        // with custom key
        double key = 122334545.0;
        assertThatThrownBy(() -> cache.get(key, k -> null).await().indefinitely())
                .isInstanceOf(IllegalArgumentException.class);
        assertThatTheKeyDoesNotExist("cache:default-redis-cache:" + Json.encode(key));
    }

    @Test
    public void testExceptionInValueLoader() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(10));
        info.valueType = Person.class;
        info.keyType = Double.class;
        info.name = "foo";
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        // with custom key and exception
        Double key = 122334545.0;
        RuntimeException thrown = new RuntimeException();

        // when exception thrown in the value loader for the key
        assertThatThrownBy(() -> {
            cache.get(key, k -> {
                throw thrown;
            }).await().indefinitely();
        }).isInstanceOf(RuntimeException.class).isEqualTo(thrown);

        assertThatTheKeyDoesNotExist(Json.encode("cache:foo:" + key));
    }

    @Test
    public void testPutShouldPopulateCache() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(10));
        info.valueType = Person.class;
        info.keyType = Integer.class;
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        cache.put(1, new Person("luke", "skywalker")).await().indefinitely();
        assertThat(cache.get(1, x -> new Person("1", "1")).await().indefinitely()).isEqualTo(new Person("luke", "skywalker"));
        assertThatTheKeyDoesExist("cache:default-redis-cache:1");
        cache.invalidate(1).await().indefinitely();
        assertThat(cache.getOrNull(1, Person.class).await().indefinitely()).isNull();
        assertThatTheKeyDoesNotExist("cache:default-redis-cache:1");
    }

    @Test
    public void testPutShouldPopulateCacheWithOptimisticLocking() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(10));
        info.valueType = Person.class;
        info.keyType = Integer.class;
        info.useOptimisticLocking = true;
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        cache.put(1, new Person("luke", "skywalker")).await().indefinitely();
        assertThat(cache.get(1, x -> new Person("1", "1")).await().indefinitely()).isEqualTo(new Person("luke", "skywalker"));
        assertThatTheKeyDoesExist("cache:default-redis-cache:1");
        cache.invalidate(1).await().indefinitely();
        assertThat(cache.getOrNull(1, Person.class).await().indefinitely()).isNull();
        assertThatTheKeyDoesNotExist("cache:default-redis-cache:1");
    }

    @Test
    public void testThatConnectionsAreRecycled() {
        String k = UUID.randomUUID().toString();
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "foo";
        info.valueType = String.class;
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(1));
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        for (int i = 0; i < 1000; i++) {
            String val = "hello-" + i;
            cache.get(k, s -> val).await().indefinitely();
        }
        var r = redis.send(Request.cmd(Command.GET).arg("cache:foo:" + k)).await().indefinitely();
        assertThat(r).isNotNull();
        assertThat(r.toString()).startsWith("hello-");

        await().untilAsserted(() -> assertThatTheKeyDoesNotExist("cache:foo:" + k));
        for (int i = 1000; i < 2000; i++) {
            String val = "hello-" + i;
            cache.get(k, s -> val).await().indefinitely();
        }
        assertThat(r.toString()).startsWith("hello-");
    }

    @Test
    public void testThatConnectionsAreRecycledWithOptimisticLocking() {
        String k = UUID.randomUUID().toString();
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "foo";
        info.valueType = String.class;
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(1));
        info.useOptimisticLocking = true;
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        for (int i = 0; i < 1000; i++) {
            String val = "hello-" + i;
            cache.get(k, s -> val).await().indefinitely();
        }
        var r = redis.send(Request.cmd(Command.GET).arg("cache:foo:" + k)).await().indefinitely();
        assertThat(r).isNotNull();
        assertThat(r.toString()).startsWith("hello-");

        await().untilAsserted(() -> assertThatTheKeyDoesNotExist("cache:foo:" + k));
        for (int i = 1000; i < 2000; i++) {
            String val = "hello-" + i;
            cache.get(k, s -> val).await().indefinitely();
        }
        assertThat(r.toString()).startsWith("hello-");
    }

    @Test
    void testWithMissingDefaultType() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "missing-default-cache";
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(10));
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        assertThatThrownBy(() -> cache.get("test", x -> "value").await().indefinitely())
                .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> cache.getAsync("test", x -> Uni.createFrom().item("value")).await().indefinitely())
                .isInstanceOf(UnsupportedOperationException.class);

        assertThat(cache.get("test", String.class, x -> "value").await().indefinitely()).isEqualTo("value");
        assertThat(cache.getAsync("test-async", String.class, x -> Uni.createFrom().item("value")).await().indefinitely())
                .isEqualTo("value");

        assertThat(cache.get("test", String.class, x -> "another").await().indefinitely()).isEqualTo("value");
        assertThat(cache.getAsync("test-async", String.class, x -> Uni.createFrom().item("another")).await().indefinitely())
                .isEqualTo("value");
    }

    @Test
    void testAsyncGetWithDefaultType() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "star-wars";
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(2));
        info.valueType = Person.class;
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        assertThat(cache
                .getAsync("test",
                        x -> Uni.createFrom().item(new Person("luke", "skywalker"))
                                .runSubscriptionOn(Infrastructure.getDefaultExecutor()))
                .await().indefinitely()).satisfies(p -> {
                    assertThat(p.firstName).isEqualTo("luke");
                    assertThat(p.lastName).isEqualTo("skywalker");
                });

        assertThat(cache.getAsync("test", x -> Uni.createFrom().item(new Person("leia", "organa")))
                .await().indefinitely()).satisfies(p -> {
                    assertThat(p.firstName).isEqualTo("luke");
                    assertThat(p.lastName).isEqualTo("skywalker");
                });

        await().untilAsserted(() -> assertThat(cache.getAsync("test", x -> Uni.createFrom().item(new Person("leia", "organa")))
                .await().indefinitely()).satisfies(p -> {
                    assertThat(p.firstName).isEqualTo("leia");
                    assertThat(p.lastName).isEqualTo("organa");
                }));
    }

    @Test
    void testAsyncGetWithDefaultTypeWithoutRedis() {
        // must start our own Redis server, because we need to stop it in the middle of the test
        GenericContainer<?> server = new GenericContainer<>("redis:7").withExposedPorts(6379);
        server.start();
        Redis redis = Redis.createClient(vertx, "redis://" + server.getHost() + ":" + server.getFirstMappedPort());

        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "star-wars";
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(2));
        info.valueType = Person.class;
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        server.close();

        assertThat(cache
                .getAsync("test",
                        x -> Uni.createFrom().item(new Person("luke", "skywalker"))
                                .runSubscriptionOn(Infrastructure.getDefaultExecutor()))
                .await().indefinitely()).satisfies(p -> {
                    assertThat(p.firstName).isEqualTo("luke");
                    assertThat(p.lastName).isEqualTo("skywalker");
                });

        assertThat(cache.getAsync("test", x -> Uni.createFrom().item(new Person("leia", "organa")))
                .await().indefinitely()).satisfies(p -> {
                    assertThat(p.firstName).isEqualTo("leia");
                    assertThat(p.lastName).isEqualTo("organa");
                });

        redis.close();
    }

    @Test
    void testAsyncGetWithDefaultTypeWithOptimisticLocking() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "star-wars";
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(2));
        info.valueType = Person.class;
        info.useOptimisticLocking = true;
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        assertThat(cache
                .getAsync("test",
                        x -> Uni.createFrom().item(new Person("luke", "skywalker"))
                                .runSubscriptionOn(Infrastructure.getDefaultExecutor()))
                .await().indefinitely()).satisfies(p -> {
                    assertThat(p.firstName).isEqualTo("luke");
                    assertThat(p.lastName).isEqualTo("skywalker");
                });

        assertThat(cache.getAsync("test", x -> Uni.createFrom().item(new Person("leia", "organa")))
                .await().indefinitely()).satisfies(p -> {
                    assertThat(p.firstName).isEqualTo("luke");
                    assertThat(p.lastName).isEqualTo("skywalker");
                });

        await().untilAsserted(() -> assertThat(cache.getAsync("test", x -> Uni.createFrom().item(new Person("leia", "organa")))
                .await().indefinitely()).satisfies(p -> {
                    assertThat(p.firstName).isEqualTo("leia");
                    assertThat(p.lastName).isEqualTo("organa");
                }));
    }

    @Test
    void testPut() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "put";
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(2));
        info.valueType = Person.class;
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        Person luke = new Person("luke", "skywalker");
        Person leia = new Person("leia", "organa");
        cache.put("test", luke).await().indefinitely();
        assertThatTheKeyDoesExist("cache:put:test");

        assertThat(cache.get("test", x -> new Person("x", "x")).await().indefinitely()).isEqualTo(luke);

        await().untilAsserted(() -> assertThat(cache.getAsync("test", x -> Uni.createFrom().item(leia))
                .await().indefinitely()).isEqualTo(leia));
    }

    @Test
    void testPutWithSupplier() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "put";
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(2));
        info.valueType = Person.class;
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        Person luke = new Person("luke", "skywalker");
        Person leia = new Person("leia", "organa");
        cache.put("test", () -> luke).await().indefinitely();
        assertThatTheKeyDoesExist("cache:put:test");

        assertThat(cache.get("test", x -> new Person("x", "x")).await().indefinitely()).isEqualTo(luke);

        await().untilAsserted(() -> assertThat(cache.get("test", x -> leia)
                .await().indefinitely()).isEqualTo(leia));
    }

    @Test
    void testGetDefaultKey() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "test-default-key";
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(2));

        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);
        assertThat(cache.getDefaultKey()).isEqualTo("default-cache-key");

        assertThat(cache.getDefaultValueType()).isNull();
    }

    @Test
    void testInvalidation() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "test-invalidation";
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(10));

        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        redis.send(Request.cmd(Command.SET).arg("key6").arg("my-value")).await().indefinitely();

        cache.put("key1", "val1").await().indefinitely();
        cache.put("key2", "val2").await().indefinitely();
        cache.put("key3", "val3").await().indefinitely();
        cache.put("key4", "val4").await().indefinitely();
        cache.put("key5", "val5").await().indefinitely();

        cache.put("clé-1", "valeur-1").await().indefinitely();
        cache.put("clé-2", "valeur-2").await().indefinitely();
        cache.put("clé-3", "valeur-3").await().indefinitely();

        cache.put("special", "special").await().indefinitely();

        assertThat(getAllKeys()).hasSize(10);

        cache.invalidate("special").await().indefinitely();
        assertThatTheKeyDoesNotExist("cache:test-invalidation:special");

        assertThat(getAllKeys()).hasSize(9);

        cache.invalidateIf(o -> o instanceof String s && s.startsWith("key")).await().indefinitely();
        assertThatTheKeyDoesNotExist("cache:test-invalidation:key1");
        assertThatTheKeyDoesNotExist("cache:test-invalidation:key2");
        assertThatTheKeyDoesExist("key6");
        assertThat(getAllKeys()).hasSize(4);

        cache.invalidateAll().await().indefinitely();
        assertThatTheKeyDoesNotExist("cache:test-invalidation:clé-1");
        assertThatTheKeyDoesNotExist("cache:test-invalidation:clé-2");
        assertThatTheKeyDoesNotExist("cache:test-invalidation:clé-3");
        assertThatTheKeyDoesExist("key6");
        assertThat(getAllKeys()).hasSize(1);
    }

    // ---- Bulk operations: getAll (no loader) ----------------------------------------

    @Test
    void testGetAll_allHits() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "bulk";
        info.valueType = String.class;
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(10));
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        cache.put("k1", "v1").await().indefinitely();
        cache.put("k2", "v2").await().indefinitely();
        cache.put("k3", "v3").await().indefinitely();

        Map<String, String> result = cache.getAll(List.of("k1", "k2", "k3"), String.class).await().indefinitely();
        assertThat(result).hasSize(3)
                .containsEntry("k1", "v1")
                .containsEntry("k2", "v2")
                .containsEntry("k3", "v3");
    }

    @Test
    void testGetAll_allMisses_noLoader() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "bulk";
        info.valueType = String.class;
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(10));
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        Map<String, String> result = cache.getAll(List.of("k1", "k2", "k3"), String.class).await().indefinitely();
        assertThat(result).isEmpty();
    }

    @Test
    void testGetAll_mixed_noLoader() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "bulk";
        info.valueType = String.class;
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(10));
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        cache.put("k1", "v1").await().indefinitely();
        cache.put("k3", "v3").await().indefinitely();

        Map<String, String> result = cache.getAll(List.of("k1", "k2", "k3"), String.class).await().indefinitely();
        assertThat(result).hasSize(2)
                .containsEntry("k1", "v1")
                .containsEntry("k3", "v3")
                .doesNotContainKey("k2");
    }

    @Test
    void testGetAll_emptyCollection() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "bulk";
        info.valueType = String.class;
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(10));
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        Map<String, String> result = cache.getAll(List.<String> of(), String.class).await().indefinitely();
        assertThat(result).isEmpty();
    }

    @Test
    void testGetAll_withExpireAfterAccess() throws InterruptedException {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "bulk-access";
        info.valueType = String.class;
        info.expireAfterAccess = Optional.of(Duration.ofSeconds(1));
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        cache.put("k1", "v1").await().indefinitely();
        cache.put("k2", "v2").await().indefinitely();

        // First getAll — should trigger a batch of EXPIRE commands for each hit key
        Map<String, String> result = cache.getAll(List.of("k1", "k2"), String.class).await().indefinitely();
        assertThat(result).hasSize(2).containsEntry("k1", "v1").containsEntry("k2", "v2");

        // TTL should have been set via the EXPIRE command issued for each hit
        Response ttl = redis.send(Request.cmd(Command.TTL).arg("cache:bulk-access:k1")).await().indefinitely();
        assertThat(ttl.toLong()).isGreaterThan(0L);

        // Repeated access within the TTL window resets the expiry each time
        for (int i = 0; i < 3; i++) {
            Thread.sleep(500);
            Map<String, String> refreshed = cache.getAll(List.of("k1", "k2"), String.class).await().indefinitely();
            assertThat(refreshed).hasSize(2);
        }

        // After access stops the key should eventually expire.
        // Use a direct Redis GET (not cache.getOrNull) because getOrNull issues GETEX which would
        // reset the TTL on every poll and the key would never expire within the polling window.
        await().until(
                () -> redis.send(Request.cmd(Command.GET).arg("cache:bulk-access:k1")).await().indefinitely() == null);
    }

    // ---- Bulk operations: getAll (with sync loader) ---------------------------------

    @Test
    void testGetAll_allMisses_withLoader() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "bulk";
        info.valueType = String.class;
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(10));
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        Map<String, String> result = cache.getAll(
                List.of("k1", "k2", "k3"),
                String.class,
                misses -> {
                    Map<String, String> loaded = new LinkedHashMap<>();
                    for (String k : misses) {
                        loaded.put(k, k.toUpperCase());
                    }
                    return loaded;
                }).await().indefinitely();

        assertThat(result).hasSize(3)
                .containsEntry("k1", "K1")
                .containsEntry("k2", "K2")
                .containsEntry("k3", "K3");

        // Values must have been written back to Redis
        assertThatTheKeyDoesExist("cache:bulk:k1");
        assertThatTheKeyDoesExist("cache:bulk:k2");
        assertThatTheKeyDoesExist("cache:bulk:k3");
    }

    @Test
    void testGetAll_mixed_withLoader() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "bulk";
        info.valueType = String.class;
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(10));
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        cache.put("k1", "cached-v1").await().indefinitely();
        cache.put("k3", "cached-v3").await().indefinitely();

        AtomicInteger loaderCallCount = new AtomicInteger(0);
        Map<String, String> result = cache.getAll(
                List.of("k1", "k2", "k3"),
                String.class,
                misses -> {
                    loaderCallCount.incrementAndGet();
                    // Loader should only receive the miss key (k2)
                    Map<String, String> loaded = new LinkedHashMap<>();
                    for (String k : misses) {
                        loaded.put(k, "loaded-" + k);
                    }
                    return loaded;
                }).await().indefinitely();

        assertThat(loaderCallCount.get()).isEqualTo(1);
        assertThat(result).hasSize(3)
                .containsEntry("k1", "cached-v1")
                .containsEntry("k2", "loaded-k2")
                .containsEntry("k3", "cached-v3");
    }

    @Test
    void testGetAll_expireAfterWrite_loaderWriteback() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "bulk";
        info.valueType = String.class;
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(1));
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        // Load via the value loader — the write-back must honour expireAfterWrite
        cache.getAll(
                List.of("k1", "k2"),
                String.class,
                misses -> {
                    Map<String, String> loaded = new LinkedHashMap<>();
                    for (String k : misses) {
                        loaded.put(k, k.toUpperCase());
                    }
                    return loaded;
                }).await().indefinitely();

        assertThatTheKeyDoesExist("cache:bulk:k1");

        // Written-back entries must expire after the configured duration
        await().until(() -> cache.getOrNull("k1", String.class).await().indefinitely() == null);
    }

    @Test
    void testGetAll_nullValueInLoader_throws() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "bulk";
        info.valueType = String.class;
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(10));
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        assertThatThrownBy(() -> cache.getAll(
                List.of("k1", "k2"),
                String.class,
                misses -> {
                    Map<String, String> loaded = new LinkedHashMap<>();
                    loaded.put("k1", "v1");
                    loaded.put("k2", null); // null value — must throw
                    return loaded;
                }).await().indefinitely())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testGetAll_loaderNotCalledOnFullHit() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "bulk";
        info.valueType = String.class;
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(10));
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        cache.put("k1", "v1").await().indefinitely();
        cache.put("k2", "v2").await().indefinitely();

        AtomicInteger loaderCallCount = new AtomicInteger(0);
        Map<String, String> result = cache.getAll(
                List.of("k1", "k2"),
                String.class,
                misses -> {
                    loaderCallCount.incrementAndGet();
                    return Map.of(); // should never be called
                }).await().indefinitely();

        assertThat(loaderCallCount.get()).isEqualTo(0);
        assertThat(result).containsEntry("k1", "v1").containsEntry("k2", "v2");
    }

    // ---- Bulk operations: getAllAsync (with async loader) ----------------------------

    @Test
    void testGetAllAsync_allMisses() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "bulk";
        info.valueType = String.class;
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(10));
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        Map<String, String> result = cache.getAllAsync(
                List.of("k1", "k2", "k3"),
                String.class,
                misses -> {
                    Map<String, String> loaded = new LinkedHashMap<>();
                    for (String k : misses) {
                        loaded.put(k, "async-" + k);
                    }
                    return Uni.createFrom().item(loaded);
                }).await().indefinitely();

        assertThat(result).hasSize(3)
                .containsEntry("k1", "async-k1")
                .containsEntry("k2", "async-k2")
                .containsEntry("k3", "async-k3");

        // Values must have been written back to Redis
        assertThatTheKeyDoesExist("cache:bulk:k1");
    }

    @Test
    void testGetAllAsync_mixed() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "bulk";
        info.valueType = String.class;
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(10));
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        cache.put("k1", "cached-v1").await().indefinitely();

        AtomicInteger loaderCallCount = new AtomicInteger(0);
        Map<String, String> result = cache.getAllAsync(
                List.of("k1", "k2"),
                String.class,
                misses -> {
                    loaderCallCount.incrementAndGet();
                    Map<String, String> loaded = new LinkedHashMap<>();
                    for (String k : misses) {
                        loaded.put(k, "async-" + k);
                    }
                    return Uni.createFrom().item(loaded);
                }).await().indefinitely();

        assertThat(loaderCallCount.get()).isEqualTo(1);
        assertThat(result).hasSize(2)
                .containsEntry("k1", "cached-v1")
                .containsEntry("k2", "async-k2");
    }

    // ---- Optimistic locking guard ---------------------------------------------------

    @Test
    void testGetAll_optimisticLocking_throws() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "bulk";
        info.valueType = String.class;
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(10));
        info.useOptimisticLocking = true;
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        assertThatThrownBy(() -> cache.getAll(
                List.of("k1"),
                String.class,
                misses -> Map.of("k1", "v1"))
                .await().indefinitely())
                .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> cache.getAllAsync(
                List.of("k1"),
                String.class,
                misses -> Uni.createFrom().item(Map.of("k1", "v1")))
                .await().indefinitely())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // ---- Bulk operations: putAll ----------------------------------------------------

    @Test
    void testPutAll_noTTL() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "bulk";
        info.valueType = String.class;
        // No expireAfterWrite — atomic MSET path
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("k1", "v1");
        entries.put("k2", "v2");
        entries.put("k3", "v3");
        cache.putAll(entries).await().indefinitely();

        assertThatTheKeyDoesExist("cache:bulk:k1");
        assertThatTheKeyDoesExist("cache:bulk:k2");
        assertThatTheKeyDoesExist("cache:bulk:k3");

        assertThat(cache.getOrNull("k1", String.class).await().indefinitely()).isEqualTo("v1");
        assertThat(cache.getOrNull("k2", String.class).await().indefinitely()).isEqualTo("v2");
        assertThat(cache.getOrNull("k3", String.class).await().indefinitely()).isEqualTo("v3");

        // Keys stored via MSET should have no expiry (TTL == -1)
        Response ttl = redis.send(Request.cmd(Command.TTL).arg("cache:bulk:k1")).await().indefinitely();
        assertThat(ttl.toLong()).isEqualTo(-1L);
    }

    @Test
    void testPutAll_withTTL() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "bulk";
        info.valueType = String.class;
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(1));
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("k1", "v1");
        entries.put("k2", "v2");
        cache.putAll(entries).await().indefinitely();

        assertThatTheKeyDoesExist("cache:bulk:k1");
        assertThatTheKeyDoesExist("cache:bulk:k2");

        // Keys stored via pipelined SET EX should carry the configured TTL
        Response ttl = redis.send(Request.cmd(Command.TTL).arg("cache:bulk:k1")).await().indefinitely();
        assertThat(ttl.toLong()).isGreaterThan(0L);

        // Keys must expire after the configured duration
        await().until(() -> cache.getOrNull("k1", String.class).await().indefinitely() == null);
        await().until(() -> cache.getOrNull("k2", String.class).await().indefinitely() == null);
    }

    @Test
    void testPutAll_thenGetAll() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "bulk";
        info.valueType = String.class;
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(10));
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("k1", "v1");
        entries.put("k2", "v2");
        entries.put("k3", "v3");
        cache.putAll(entries).await().indefinitely();

        Map<String, String> result = cache.getAll(List.of("k1", "k2", "k3"), String.class).await().indefinitely();
        assertThat(result).isEqualTo(entries);
    }

    @Test
    void testPutAll_nullValue_throws() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "bulk";
        info.valueType = String.class;
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(10));
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        // TTL path (pipelined SET EX)
        Map<String, String> withNull = new LinkedHashMap<>();
        withNull.put("k1", "v1");
        withNull.put("k2", null);
        assertThatThrownBy(() -> cache.putAll(withNull).await().indefinitely())
                .isInstanceOf(IllegalArgumentException.class);

        // No-TTL path (MSET)
        RedisCacheInfo infoNoTtl = new RedisCacheInfo();
        infoNoTtl.name = "bulk-no-ttl";
        infoNoTtl.valueType = String.class;
        RedisCacheImpl cacheNoTtl = new RedisCacheImpl(infoNoTtl, vertx, redis, BLOCKING_ALLOWED);

        Map<String, String> withNull2 = new LinkedHashMap<>();
        withNull2.put("k1", "v1");
        withNull2.put("k2", null);
        assertThatThrownBy(() -> cacheNoTtl.putAll(withNull2).await().indefinitely())
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- Type-specialised bulk tests ------------------------------------------------

    @Test
    void testGetAll_customKeyType() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "bulk";
        info.valueType = String.class;
        info.keyType = Integer.class;
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(10));
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        cache.put(1, "one").await().indefinitely();
        cache.put(2, "two").await().indefinitely();
        cache.put(3, "three").await().indefinitely();

        Map<Integer, String> result = cache.getAll(List.of(1, 2, 3), String.class).await().indefinitely();
        assertThat(result).hasSize(3)
                .containsEntry(1, "one")
                .containsEntry(2, "two")
                .containsEntry(3, "three");
    }

    @Test
    void testGetAll_customValueType_POJO() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "bulk";
        info.valueType = Person.class;
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(10));
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        Person luke = new Person("luke", "skywalker");
        Person leia = new Person("leia", "organa");
        cache.put("luke", luke).await().indefinitely();
        cache.put("leia", leia).await().indefinitely();

        Map<String, Person> result = cache.getAll(List.of("luke", "leia", "missing"), Person.class)
                .await().indefinitely();
        assertThat(result).hasSize(2)
                .containsEntry("luke", luke)
                .containsEntry("leia", leia)
                .doesNotContainKey("missing");
    }

    @Test
    void testGetAll_typeLiteral() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "bulk";
        info.valueType = List.class;
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(10));
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        TypeLiteral<List<String>> type = new TypeLiteral<>() {
        };

        cache.put("tags-alice", List.of("admin", "user")).await().indefinitely();
        cache.put("tags-bob", List.of("user")).await().indefinitely();

        // getAll with TypeLiteral — no loader
        Map<String, List<String>> result = cache.getAll(List.of("tags-alice", "tags-bob", "tags-missing"), type)
                .await().indefinitely();
        assertThat(result).hasSize(2)
                .containsEntry("tags-alice", List.of("admin", "user"))
                .containsEntry("tags-bob", List.of("user"))
                .doesNotContainKey("tags-missing");

        // getAll with TypeLiteral + sync loader
        Map<String, List<String>> withLoader = cache.getAll(
                List.of("tags-alice", "tags-carol"),
                type,
                misses -> {
                    Map<String, List<String>> loaded = new LinkedHashMap<>();
                    for (String k : misses) {
                        loaded.put(k, List.of("guest"));
                    }
                    return loaded;
                }).await().indefinitely();
        assertThat(withLoader).hasSize(2)
                .containsEntry("tags-alice", List.of("admin", "user"))
                .containsEntry("tags-carol", List.of("guest"));
    }

    // ---- Missing default type guard -------------------------------------------------

    @Test
    void testGetAll_missingDefaultType_throws() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "bulk-no-type";
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(10));
        // No valueType configured — default-type overloads must throw
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, redis, BLOCKING_ALLOWED);

        assertThatThrownBy(() -> cache.getAll(List.of("k1")).await().indefinitely())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> cache.getAll(List.of("k1"), misses -> Map.of("k1", "v1")).await().indefinitely())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> cache.getAllAsync(List.of("k1"),
                misses -> Uni.createFrom().item(Map.of("k1", "v1"))).await().indefinitely())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // ---- Redis unavailability fallback ----------------------------------------------

    @Test
    void testGetAll_redisUnavailable_fallback() {
        // Start a dedicated Redis server so it can be stopped mid-test
        GenericContainer<?> server = new GenericContainer<>("redis:7").withExposedPorts(6379);
        server.start();
        Redis localRedis = Redis.createClient(vertx,
                "redis://" + server.getHost() + ":" + server.getFirstMappedPort());

        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "bulk";
        info.valueType = String.class;
        info.expireAfterWrite = Optional.of(Duration.ofSeconds(10));
        RedisCacheImpl cache = new RedisCacheImpl(info, vertx, localRedis, BLOCKING_ALLOWED);

        server.close(); // Bring Redis down before the bulk read

        AtomicInteger loaderCallCount = new AtomicInteger(0);
        Map<String, String> result = cache.getAll(
                List.of("k1", "k2"),
                String.class,
                misses -> {
                    loaderCallCount.incrementAndGet();
                    Map<String, String> loaded = new LinkedHashMap<>();
                    for (String k : misses) {
                        loaded.put(k, "fallback-" + k);
                    }
                    return loaded;
                }).await().indefinitely();

        // Loader must have been called once with all keys (full fallback)
        assertThat(loaderCallCount.get()).isEqualTo(1);
        assertThat(result).hasSize(2)
                .containsEntry("k1", "fallback-k1")
                .containsEntry("k2", "fallback-k2");

        localRedis.close();
    }

    private Set<String> getAllKeys() {
        return redis.send(Request.cmd(Command.KEYS).arg("*"))
                .map(r -> {
                    Set<String> keys = new HashSet<>();
                    for (Response response : r) {
                        keys.add(response.toString());
                    }
                    return keys;
                })
                .await().indefinitely();
    }

    private void assertThatTheKeyDoesExist(String key) {
        var actualKeySet = getAllKeys();
        assertThat(actualKeySet).contains(key);
    }

    private void assertThatTheKeyDoesNotExist(String key) {
        assertThat(getAllKeys()).doesNotContain(key);
    }

}
