package io.quarkus.cache.redis.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.json.Json;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Request;
import io.vertx.mutiny.redis.client.Response;

class RedisCacheImplTest extends RedisCacheTestBase {

    @AfterEach
    void clear() {
        redis.send(Request.cmd(Command.FLUSHALL).arg("SYNC")).await().atMost(Duration.ofSeconds(10));
    }

    @Test
    public void testPutInTheCache() {
        String k = UUID.randomUUID().toString();
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "foo";
        info.valueType = String.class.getName();
        info.ttl = Optional.of(Duration.ofSeconds(2));
        RedisCacheImpl<String, String> cache = new RedisCacheImpl<>(info, redis);
        assertThat(cache.get(k, s -> "hello").await().indefinitely()).isEqualTo("hello");
        var r = redis.send(Request.cmd(Command.GET).arg("cache:foo:" + k)).await().indefinitely();
        assertThat(r).isNotNull();
    }

    @Test
    public void testPutInTheCacheWithOptimisitcLocking() {
        String k = UUID.randomUUID().toString();
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "foo";
        info.valueType = String.class.getName();
        info.ttl = Optional.of(Duration.ofSeconds(2));
        info.useOptimisticLocking = true;
        RedisCacheImpl<String, String> cache = new RedisCacheImpl<>(info, redis);
        assertThat(cache.get(k, s -> "hello").await().indefinitely()).isEqualTo("hello");
        var r = redis.send(Request.cmd(Command.GET).arg("cache:foo:" + k)).await().indefinitely();
        assertThat(r).isNotNull();
    }

    @Test
    public void testPutAndWaitForInvalidation() {
        String k = UUID.randomUUID().toString();
        RedisCacheInfo info = new RedisCacheInfo();
        info.valueType = String.class.getName();
        info.ttl = Optional.of(Duration.ofSeconds(1));
        RedisCacheImpl<String, String> cache = new RedisCacheImpl<>(info, redis);
        assertThat(cache.get(k, s -> "hello").await().indefinitely()).isEqualTo("hello");
        var x = cache.get(k, String::toUpperCase).await().indefinitely();
        assertEquals(x, "hello");
        await().until(() -> cache.getOrNull(k, String.class).await().indefinitely() == null);
    }

    @Test
    public void testManualInvalidation() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.valueType = String.class.getName();
        info.ttl = Optional.of(Duration.ofSeconds(10));
        RedisCacheImpl<String, String> cache = new RedisCacheImpl<>(info, redis);
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
        info.ttl = Optional.of(Duration.ofSeconds(10));
        info.valueType = Person.class.getName();
        RedisCacheImpl<String, Person> cache = new RedisCacheImpl<>(info, redis);
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
        info.ttl = Optional.of(Duration.ofSeconds(10));
        info.valueType = Person.class.getName();
        RedisCacheImpl<String, Person> cache = new RedisCacheImpl<>(info, redis);
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
        info.ttl = Optional.of(Duration.ofSeconds(10));
        info.valueType = Person.class.getName();
        RedisCacheImpl<String, Person> cache = new RedisCacheImpl<>(info, redis);

        // with custom key
        double key = 122334545.0;
        assertThatThrownBy(() -> cache.get(key, k -> null).await().indefinitely())
                .isInstanceOf(IllegalArgumentException.class);
        assertThatTheKeyDoesNotExist("cache:default-redis-cache:" + Json.encode(key));
    }

    @Test
    public void testExceptionInValueLoader() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.ttl = Optional.of(Duration.ofSeconds(10));
        info.valueType = Person.class.getName();
        info.keyType = Double.class.getName();
        info.name = "foo";
        RedisCacheImpl<Double, Person> cache = new RedisCacheImpl<>(info, redis);

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
        info.ttl = Optional.of(Duration.ofSeconds(10));
        info.valueType = Person.class.getName();
        info.keyType = Integer.class.getName();
        RedisCacheImpl<Integer, Person> cache = new RedisCacheImpl<>(info, redis);

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
        info.ttl = Optional.of(Duration.ofSeconds(10));
        info.valueType = Person.class.getName();
        info.keyType = Integer.class.getName();
        info.useOptimisticLocking = true;
        RedisCacheImpl<Integer, Person> cache = new RedisCacheImpl<>(info, redis);

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
        info.valueType = String.class.getName();
        info.ttl = Optional.of(Duration.ofSeconds(1));
        RedisCacheImpl<String, String> cache = new RedisCacheImpl<>(info, redis);

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
        info.valueType = String.class.getName();
        info.ttl = Optional.of(Duration.ofSeconds(1));
        info.useOptimisticLocking = true;
        RedisCacheImpl<String, String> cache = new RedisCacheImpl<>(info, redis);

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
        info.ttl = Optional.of(Duration.ofSeconds(10));
        RedisCacheImpl<String, String> cache = new RedisCacheImpl<>(info, redis);

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
        info.ttl = Optional.of(Duration.ofSeconds(2));
        info.valueType = Person.class.getName();
        RedisCacheImpl<String, Person> cache = new RedisCacheImpl<>(info, redis);

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
    void testAsyncGetWithDefaultTypeWithOptimisticLocking() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "star-wars";
        info.ttl = Optional.of(Duration.ofSeconds(2));
        info.valueType = Person.class.getName();
        info.useOptimisticLocking = true;
        RedisCacheImpl<String, Person> cache = new RedisCacheImpl<>(info, redis);

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
        info.ttl = Optional.of(Duration.ofSeconds(2));
        info.valueType = Person.class.getName();
        RedisCacheImpl<String, Person> cache = new RedisCacheImpl<>(info, redis);

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
        info.ttl = Optional.of(Duration.ofSeconds(2));
        info.valueType = Person.class.getName();
        RedisCacheImpl<String, Person> cache = new RedisCacheImpl<>(info, redis);

        Person luke = new Person("luke", "skywalker");
        Person leia = new Person("leia", "organa");
        cache.put("test", () -> luke).await().indefinitely();
        assertThatTheKeyDoesExist("cache:put:test");

        assertThat(cache.get("test", x -> new Person("x", "x")).await().indefinitely()).isEqualTo(luke);

        await().untilAsserted(() -> assertThat(cache.get("test", x -> leia)
                .await().indefinitely()).isEqualTo(leia));
    }

    @Test
    void testInitializationWithAnUnknownClass() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "put";
        info.ttl = Optional.of(Duration.ofSeconds(2));
        info.valueType = Person.class.getPackage().getName() + ".Missing";

        assertThatThrownBy(() -> new RedisCacheImpl<>(info, redis))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testGetDefaultKey() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "test-default-key";
        info.ttl = Optional.of(Duration.ofSeconds(2));

        RedisCacheImpl<String, String> cache = new RedisCacheImpl<>(info, redis);
        assertThat(cache.getDefaultKey()).isEqualTo("default-cache-key");

        assertThat(cache.getDefaultValueType()).isNull();
    }

    @Test
    void testInvalidation() {
        RedisCacheInfo info = new RedisCacheInfo();
        info.name = "test-invalidation";
        info.ttl = Optional.of(Duration.ofSeconds(10));

        RedisCacheImpl<String, String> cache = new RedisCacheImpl<>(info, redis);

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

        cache.invalidateIf(o -> o instanceof String && ((String) o).startsWith("key")).await().indefinitely();
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
