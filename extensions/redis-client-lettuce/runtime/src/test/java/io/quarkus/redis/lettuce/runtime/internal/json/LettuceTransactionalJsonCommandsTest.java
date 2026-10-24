package io.quarkus.redis.lettuce.runtime.internal.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.json.ReactiveTransactionalJsonCommands;
import io.quarkus.redis.datasource.json.TransactionalJsonCommands;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;
import io.quarkus.redis.lettuce.runtime.internal.Person;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

class LettuceTransactionalJsonCommandsTest extends CommandsTestBase {

    ReactiveRedisDataSource reactiveDs;
    RedisDataSource blockingDs;

    Person person = new Person("luke", "skywalker");
    Person person2 = new Person("leia", "skywalker");
    Person person3 = new Person("anakin", "skywalker");

    @BeforeEach
    void initialize() {
        reactiveDs = reactiveDataSource();
        blockingDs = blockingDataSource(Duration.ofSeconds(60));
    }

    @SuppressWarnings("unchecked")
    @Test
    void setBlocking() {
        TransactionResult result = blockingDs.withTransaction(tx -> {
            TransactionalJsonCommands<String> json = tx.json();
            assertThat(json.getDataSource()).isEqualTo(tx);
            json.jsonSet(key, "$", person); // 0
            json.jsonSet(key, "$.sister", person2); // 1
            json.jsonSet(key, "$.a", JsonArray.of(1, 2, 3, 4)); // 2

            json.jsonArrTrim(key, "$.a", 0, 2); // 3 -> [3], array is now [1, 2, 3]
            json.jsonArrPop(key, Integer.class, "$.a", -1); // 4 -> [3]
            json.jsonArrLen(key, "$.a"); // 5 -> [2]
            json.jsonClear(key, "$.a"); // 6 -> 1

            json.jsonStrLen(key, "$.sister.lastname"); // 7 -> [9]
            json.jsonStrAppend(key, "$.sister.lastname", "!"); // 8 -> [10]
            json.jsonStrLen(key, "$.sister.lastname"); // 9 -> [10]

            json.jsonGet(key); // 10 {...}

            json.jsonSet("sister", "$", new JsonObject(Json.encode(person2))); // 11
            json.jsonGet("sister", Person.class); // 12

            json.jsonSet("someone", person3); // 13
            json.jsonGetObject("someone"); // 14
        });
        assertThat(result.size()).isEqualTo(15);
        assertThat(result.discarded()).isFalse();

        assertThat((Void) result.get(0)).isNull();
        assertThat((Void) result.get(1)).isNull();
        assertThat((Void) result.get(2)).isNull();
        assertThat((List<Integer>) result.get(3)).containsExactly(3);
        assertThat((List<Integer>) result.get(4)).containsExactly(3);
        assertThat((List<Integer>) result.get(5)).containsExactly(2);
        assertThat((int) result.get(6)).isEqualTo(1);
        assertThat((List<Integer>) result.get(7)).containsExactly(person2.lastname.length());
        assertThat((List<Integer>) result.get(8)).containsExactly(person2.lastname.length() + 1);
        assertThat((List<Integer>) result.get(9)).containsExactly(person2.lastname.length() + 1);
        JsonObject actual = result.get(10);
        assertThat(actual.getString("firstname")).isEqualTo(person.firstname);
        assertThat(actual.getString("lastname")).isEqualTo(person.lastname);
        assertThat(actual.getJsonObject("sister").getString("lastname")).isEqualTo(person2.lastname + "!");
        assertThat(actual.getJsonArray("a")).isEmpty(); // cleared
        assertThat((Void) result.get(11)).isNull();
        assertThat((Person) result.get(12)).isEqualTo(person2);
        assertThat((Void) result.get(13)).isNull();
        assertThat(((JsonObject) result.get(14)).mapTo(Person.class)).isEqualTo(person3);
    }

    @SuppressWarnings("unchecked")
    @Test
    void setReactive() {
        TransactionResult result = reactiveDs.withTransaction(tx -> {
            ReactiveTransactionalJsonCommands<String> json = tx.json();
            assertThat(json.getDataSource()).isEqualTo(tx);
            return json.jsonSet(key, "$", person) // 0
                    .chain(() -> json.jsonSet(key, "$.sister", person2)) // 1
                    .chain(() -> json.jsonSet(key, "$.a", JsonArray.of(1, 2, 3, 4))) // 2
                    .chain(() -> json.jsonArrTrim(key, "$.a", 0, 2)) // 3 -> [3], array is now [1, 2, 3]
                    .chain(() -> json.jsonArrPop(key, Integer.class, "$.a", -1)) // 4 -> [3]
                    .chain(() -> json.jsonArrLen(key, "$.a")) // 5 -> [2]
                    .chain(() -> json.jsonClear(key, "$.a")) // 6 -> 1
                    .chain(() -> json.jsonStrLen(key, "$.sister.lastname")) // 7 -> [9]
                    .chain(() -> json.jsonStrAppend(key, "$.sister.lastname", "!")) // 8 -> [10]
                    .chain(() -> json.jsonStrLen(key, "$.sister.lastname")) // 9 -> [10]
                    .chain(() -> json.jsonGet(key)) // 10 {...}
                    .chain(() -> json.jsonSet("sister", "$", new JsonObject(Json.encode(person2)))) // 11
                    .chain(() -> json.jsonGet("sister", Person.class)) // 12
                    .chain(() -> json.jsonSet("someone", person3)) // 13
                    .chain(() -> json.jsonGetObject("someone")); // 14
        }).await().atMost(Duration.ofSeconds(5));
        assertThat(result.size()).isEqualTo(15);
        assertThat(result.discarded()).isFalse();

        assertThat((Void) result.get(0)).isNull();
        assertThat((Void) result.get(1)).isNull();
        assertThat((Void) result.get(2)).isNull();
        assertThat((List<Integer>) result.get(3)).containsExactly(3);
        assertThat((List<Integer>) result.get(4)).containsExactly(3);
        assertThat((List<Integer>) result.get(5)).containsExactly(2);
        assertThat((int) result.get(6)).isEqualTo(1);
        assertThat((List<Integer>) result.get(7)).containsExactly(person2.lastname.length());
        assertThat((List<Integer>) result.get(8)).containsExactly(person2.lastname.length() + 1);
        assertThat((List<Integer>) result.get(9)).containsExactly(person2.lastname.length() + 1);
        JsonObject actual = result.get(10);
        assertThat(actual.getString("firstname")).isEqualTo(person.firstname);
        assertThat(actual.getString("lastname")).isEqualTo(person.lastname);
        assertThat(actual.getJsonObject("sister").getString("lastname")).isEqualTo(person2.lastname + "!");
        assertThat(actual.getJsonArray("a")).isEmpty(); // cleared
        assertThat((Void) result.get(11)).isNull();
        assertThat((Person) result.get(12)).isEqualTo(person2);
        assertThat((Void) result.get(13)).isNull();
        assertThat(((JsonObject) result.get(14)).mapTo(Person.class)).isEqualTo(person3);
    }

}
