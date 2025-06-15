
package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.json.ReactiveTransactionalJsonCommands;
import io.quarkus.redis.datasource.json.TransactionalJsonCommands;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;
import io.quarkus.redis.runtime.datasource.ReactiveRedisDataSourceImpl;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@RequiresCommand("json.get")
public class TransactionalJsonCommandsTest extends DatasourceTestBase {

    private RedisDataSource blocking;
    private ReactiveRedisDataSource reactive;

    Person person = new Person("luke", "skywalker");
    Person person2 = new Person("leia", "skywalker");
    Person person3 = new Person("anakin", "skywalker");

    @BeforeEach
    void initialize() {
        blocking = new BlockingRedisDataSourceImpl(vertx, redis, api, Duration.ofSeconds(60));
        reactive = new ReactiveRedisDataSourceImpl(vertx, redis, api);
    }

    @AfterEach
    public void clear() {
        blocking.flushall();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void setBlocking() {

        TransactionResult result = blocking.withTransaction(tx -> {
            TransactionalJsonCommands<String> json = tx.json();
            assertThat(json.getDataSource()).isEqualTo(tx);
            json.jsonSet(key, "$", person); // 0
            json.jsonSet(key, "$.sister", person2); // 1
            json.jsonSet(key, "$.a", JsonArray.of(1, 2, 3)); // 2

            json.jsonArrPop(key, Integer.class, "$.a", -1); // 3 -> [3]
            json.jsonArrLen(key, "$.a"); // 4 -> [2]
            json.jsonClear(key, "$.a"); // 5 -> 1

            json.jsonStrLen(key, "$.sister.lastname"); // 6 -> [9]
            json.jsonStrAppend(key, "$.sister.lastname", "!"); // 7 -> 10
            json.jsonStrLen(key, "$.sister.lastname"); // 8 -> 10

            json.jsonGet(key); // 9 {...}

            json.jsonSet("sister", "$", new JsonObject(Json.encode(person2)));
            json.jsonGet("sister", Person.class);

            json.jsonSet("someone", person3);
            json.jsonGetObject("someone");

        });
        assertThat(result.size()).isEqualTo(14);
        assertThat(result.discarded()).isFalse();

        assertThat((Void) result.get(0)).isNull();
        assertThat((Void) result.get(1)).isNull();
        assertThat((Void) result.get(2)).isNull();
        assertThat((List<Integer>) result.get(3)).containsExactly(3);
        assertThat((List<Integer>) result.get(4)).containsExactly(2);
        assertThat((int) result.get(5)).isEqualTo(1);
        assertThat((List<Integer>) result.get(6)).containsExactly(person2.lastname.length());
        assertThat((List<Integer>) result.get(7)).containsExactly(person2.lastname.length() + 1);
        assertThat((List<Integer>) result.get(8)).containsExactly(person2.lastname.length() + 1);
        JsonObject actual = result.get(9);
        assertThat(actual.getString("firstname")).isEqualTo(person.firstname);
        assertThat(actual.getString("lastname")).isEqualTo(person.lastname);
        assertThat(actual.getJsonObject("sister").getString("lastname")).isEqualTo(person2.lastname + "!");
        assertThat(actual.getJsonArray("a")).isEmpty();// cleared
        assertThat((Void) result.get(10)).isNull();
        assertThat((Person) result.get(11)).isEqualTo(person2);
        assertThat(((JsonObject) result.get(13)).mapTo(Person.class)).isEqualTo(person3);
    }

    @Test
    public void setReactive() {
        TransactionResult result = reactive.withTransaction(tx -> {
            ReactiveTransactionalJsonCommands<String> json = tx.json();
            assertThat(json.getDataSource()).isEqualTo(tx);
            return json.jsonSet(key, "$", person) // 0
                    .chain(() -> json.jsonSet(key, "$.sister", person2)) // 1
                    .chain(() -> json.jsonSet(key, "$.a", JsonArray.of(1, 2, 3))) // 2
                    .chain(() -> json.jsonArrPop(key, Integer.class, "$.a", -1)) // 3 -> [3]
                    .chain(() -> json.jsonArrLen(key, "$.a")) // 4 -> [2]
                    .chain(() -> json.jsonClear(key, "$.a")) // 5 -> 1
                    .chain(() -> json.jsonStrLen(key, "$.sister.lastname")) // 6 -> [9]
                    .chain(() -> json.jsonStrAppend(key, "$.sister.lastname", "!")) // 7 -> 10
                    .chain(() -> json.jsonStrLen(key, "$.sister.lastname")) // 8 -> 10
                    .chain(() -> json.jsonGet(key)) // 9 {...}
                    .chain(() -> json.jsonSet("sister", "$", new JsonObject(Json.encode(person2))))
                    .chain(() -> json.jsonGet("sister", Person.class)).chain(() -> json.jsonSet("someone", person3))
                    .chain(() -> json.jsonGetObject("someone"));
        }).await().atMost(Duration.ofSeconds(5));
        assertThat(result.size()).isEqualTo(14);
        assertThat(result.discarded()).isFalse();

        assertThat((Void) result.get(0)).isNull();
        assertThat((Void) result.get(1)).isNull();
        assertThat((Void) result.get(2)).isNull();
        assertThat((List<Integer>) result.get(3)).containsExactly(3);
        assertThat((List<Integer>) result.get(4)).containsExactly(2);
        assertThat((int) result.get(5)).isEqualTo(1);
        assertThat((List<Integer>) result.get(6)).containsExactly(person2.lastname.length());
        assertThat((List<Integer>) result.get(7)).containsExactly(person2.lastname.length() + 1);
        assertThat((List<Integer>) result.get(8)).containsExactly(person2.lastname.length() + 1);
        JsonObject actual = result.get(9);
        assertThat(actual.getString("firstname")).isEqualTo(person.firstname);
        assertThat(actual.getString("lastname")).isEqualTo(person.lastname);
        assertThat(actual.getJsonObject("sister").getString("lastname")).isEqualTo(person2.lastname + "!");
        assertThat(actual.getJsonArray("a")).isEmpty();// cleared
        assertThat((Void) result.get(10)).isNull();
        assertThat((Person) result.get(11)).isEqualTo(person2);
        assertThat((Person) result.get(11)).isEqualTo(person2);
        assertThat(((JsonObject) result.get(13)).mapTo(Person.class)).isEqualTo(person3);
    }

}
