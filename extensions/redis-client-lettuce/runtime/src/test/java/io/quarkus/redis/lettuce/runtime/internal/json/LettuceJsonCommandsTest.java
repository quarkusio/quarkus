package io.quarkus.redis.lettuce.runtime.internal.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.json.JsonCommands;
import io.quarkus.redis.datasource.json.JsonSetArgs;
import io.quarkus.redis.datasource.json.ReactiveJsonCommands;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

class LettuceJsonCommandsTest extends CommandsTestBase {

    ReactiveRedisDataSource reactiveDs;
    RedisDataSource blockingDs;
    ReactiveJsonCommands<String> reactiveJson;
    JsonCommands<String> blockingJson;

    @BeforeEach
    void initialize() {
        reactiveDs = reactiveDataSource();
        blockingDs = blockingDataSource();
        reactiveJson = reactiveDs.json();
        blockingJson = blockingDs.json();
    }

    @Test
    void getDataSource() {
        assertThat(blockingDs).isEqualTo(blockingJson.getDataSource());
        assertThat(reactiveDs).isEqualTo(reactiveJson.getDataSource());
    }

    @Test
    void testJson() {
        blockingJson.jsonSet("doc", "$",
                new JsonObject().put("a", 2).put("b", 3).put("nested", new JsonObject().put("a", 4).put("b", null)));
        assertThat(blockingJson.jsonGet("doc", "$..b")).containsExactly(3, null);
        JsonObject object = blockingJson.jsonGet("doc", "..a", "$..b");
        assertThat(object).hasSize(2);
        assertThat(object.getJsonArray("..a")).containsExactly(2, 4);
        assertThat(object.getJsonArray("$..b")).containsExactly(3, null);
    }

    @Test
    void jsonSetRoot() {
        blockingJson.jsonSet("animal", "$", "dog");
        assertThat(blockingJson.jsonGet("animal", "$")).containsExactly("dog");
    }

    public static class Person {
        public String name;
        public int age;

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Person person = (Person) o;
            return age == person.age && Objects.equals(name, person.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }
    }

    @Test
    void jsonSet() {
        Person p = new Person();
        p.name = "luke";
        p.age = 20;

        blockingJson.jsonSet("luke", "$", p);
        assertThat(blockingJson.jsonGet("luke", Person.class)).isEqualTo(p);

        Person p2 = new Person();
        p2.name = "leia";
        p2.age = 20;
        blockingJson.jsonSet("luke", "$.sister", p2);
        blockingJson.jsonSet("luke", "$.friends", new JsonArray().add("Obiwan").add("Han"), new JsonSetArgs().nx());
        blockingJson.jsonSet("luke", "$.enemies", new JsonArray().add("Darth Sidious"), new JsonSetArgs().nx());
        blockingJson.jsonSet("luke", "$.weapons", new JsonArray().add("Light Saber"));
        blockingJson.jsonSet("luke", "$.father", new JsonObject().put("name", "Darth Vader").put("otherName", "Anakin"));
        blockingJson.jsonSet("luke", "$.sister.father", new JsonObject().put("name", "Darth Vader").put("otherName", "Anakin"));

        JsonObject luke = blockingJson.jsonGetObject("luke");
        assertThat(luke.getString("name")).isEqualTo("luke");
        assertThat(luke.getInteger("age")).isEqualTo(20);
        assertThat(luke.getJsonArray("enemies")).containsExactly("Darth Sidious");
        assertThat(luke.getJsonArray("weapons")).containsExactly("Light Saber");
        assertThat(luke.getJsonObject("sister").getInteger("age")).isEqualTo(20);
        assertThat(luke.getJsonObject("sister").getString("name")).isEqualTo("leia");
        assertThat(luke.getJsonObject("sister").getJsonObject("father").getString("name")).isEqualTo("Darth Vader");
        assertThat(luke.getJsonArray("friends")).containsExactly("Obiwan", "Han");
        assertThat(luke.getJsonObject("father").getString("name")).isEqualTo("Darth Vader");

        blockingJson.jsonSet("luke", "$", p, new JsonSetArgs().nx());

        luke = blockingJson.jsonGetObject("luke");
        assertThat(luke.getString("name")).isEqualTo("luke");
        assertThat(luke.getInteger("age")).isEqualTo(20);
        assertThat(luke.getJsonObject("sister").getInteger("age")).isEqualTo(20);
        assertThat(luke.getJsonObject("sister").getString("name")).isEqualTo("leia");
        assertThat(luke.getJsonObject("sister").getJsonObject("father").getString("name")).isEqualTo("Darth Vader");
        assertThat(luke.getJsonArray("friends")).containsExactly("Obiwan", "Han");
        assertThat(luke.getJsonObject("father").getString("name")).isEqualTo("Darth Vader");

        blockingJson.jsonSet("luke", "$", p, new JsonSetArgs().xx());

        luke = blockingJson.jsonGetObject("luke");
        assertThat(luke.getString("name")).isEqualTo("luke");
        assertThat(luke.getInteger("age")).isEqualTo(20);
        assertThat(luke.getJsonObject("sister")).isNull();
    }

    @Test
    void jsonGetArray() {
        blockingJson.jsonSet("array", "$", List.of("a", "b", "c"));
        assertThat(blockingJson.jsonGetArray("array")).containsExactly("a", "b", "c");
    }

    @Test
    void jsonSetNull() {
        blockingJson.jsonSet("null", "$", (Object) null);
        assertThatThrownBy(() -> blockingJson.jsonSet("null-json", "$", (JsonArray) null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> blockingJson.jsonSet("null-json", "$", (JsonObject) null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(blockingJson.jsonGet("null")).isNull();
        assertThat(blockingJson.jsonGet("null", String.class)).isNull();

        assertThat(blockingJson.jsonGet("missing", String.class)).isNull();
        assertThat(blockingJson.jsonGet("missing")).isNull();
        assertThat(blockingJson.jsonGetArray("missing")).isNull();
        assertThat(blockingJson.jsonGet("missing", "$")).isNull();
    }

    @Test
    void testJsonGet() {
        Person p = new Person();
        p.name = "luke";
        p.age = 20;

        blockingJson.jsonSet("luke", "$", p);
        assertThat(blockingJson.jsonGet("luke", Person.class)).isEqualTo(p);

        Person p2 = new Person();
        p2.name = "leia";
        p2.age = 20;
        blockingJson.jsonSet("luke", "$.sister", p2);
        blockingJson.jsonSet("luke", "$.friends", new JsonArray().add("Obiwan").add("Han"), new JsonSetArgs().nx());
        blockingJson.jsonSet("luke", "$.enemies", new JsonArray().add("Darth Sidious"), new JsonSetArgs().nx());
        blockingJson.jsonSet("luke", "$.weapons", new JsonArray().add("Light Saber"));
        blockingJson.jsonSet("luke", "$.father", new JsonObject().put("name", "Darth Vader").put("otherName", "Anakin"));
        blockingJson.jsonSet("luke", "$.sister.father", new JsonObject().put("name", "Darth Vader").put("otherName", "Anakin"));

        assertThat(blockingJson.jsonGet("luke", "$")).hasSize(1);
        assertThat(blockingJson.jsonGet("luke", "$.missing")).hasSize(0);
        assertThat(blockingJson.jsonGet("luke").getString("name")).isEqualTo("luke");
        assertThat(blockingJson.jsonGet("luke", "$.friends[0]").getString(0)).isEqualTo("Obiwan");
        assertThat(blockingJson.jsonGet("luke", "$.friends[2]")).hasSize(0);
        JsonObject query = blockingJson.jsonGet("luke", "$.friends", "..father");
        assertThat(query).hasSize(2);
        assertThat(query.getJsonArray("$.friends").getJsonArray(0)).containsExactly("Obiwan", "Han");
        assertThat(query.getJsonArray("..father").getJsonObject(0).getString("name")).isEqualTo("Darth Vader");
        assertThat(query.getJsonArray("..father").getJsonObject(1).getString("name")).isEqualTo("Darth Vader");

        query = blockingJson.jsonGet("luke", "$.friends", ".missing", "..father");
        assertThat(query).hasSize(3);
        assertThat(query.getJsonArray(".missing")).isEmpty();
    }

    @Test
    void jsonArrAppend() {
        JsonObject test = JsonObject.of("a", 10, "arr", JsonArray.of(1, 2));
        JsonArray arr = JsonArray.of("a", "b", "c");
        blockingJson.jsonSet(key, "$", test);
        blockingJson.jsonSet("arr", "$", arr);
        blockingJson.jsonSet("obj", "$", JsonObject.of("name", "clement"));

        List<Integer> list = blockingJson.jsonArrAppend(key, "arr",
                new io.quarkus.redis.lettuce.runtime.internal.Person("luke", "skywalker"));
        assertThat(list).containsExactly(3); // 1, 2, luke...
        list = blockingJson.jsonArrAppend("arr", "$",
                new io.quarkus.redis.lettuce.runtime.internal.Person("luke", "skywalker"));
        assertThat(list).containsExactly(4); // a, b, c, luke...
        list = blockingJson.jsonArrAppend("arr", "$", "d", "e");
        assertThat(list).containsExactly(6);
        assertThat(blockingJson.jsonGetArray("arr").getString(4)).isEqualTo("d");
        assertThatThrownBy(() -> blockingJson.jsonArrAppend("obj", ".name", "a", "b"))
                .hasMessageContaining("Path");

        assertThatThrownBy(() -> blockingJson.jsonArrAppend("missing", ".name", "a", "b"))
                .hasMessageContaining("key");
    }

    @Test
    void jsonArrIndex() {
        JsonObject test = JsonObject.of("a", JsonArray.of(1, 2, 3, 2),
                "nested", JsonObject.of("a", JsonArray.of(3, 4)));
        blockingJson.jsonSet(key, "$", test);
        assertThat(blockingJson.jsonArrIndex(key, "$..a", 2)).containsExactly(1, -1);
        assertThat(blockingJson.jsonArrIndex(key, "$..a", 2, 0, 10)).containsExactly(1, -1);

        assertThatThrownBy(() -> blockingJson.jsonArrIndex(key, ".name", 2))
                .hasMessageContaining("Path");

        assertThatThrownBy(() -> blockingJson.jsonArrIndex("missing", ".name", "a"))
                .hasMessageContaining("Path");

        test = JsonObject.of("a", JsonArray.of(1, 2, 3, 2),
                "nested", JsonObject.of("a", false));
        blockingJson.jsonSet(key, "$", test);
        assertThat(blockingJson.jsonArrIndex(key, "$..a", 2)).containsExactly(1, null);
    }

    @Test
    void jsonArrInsert() {
        JsonObject test = JsonObject.of("a", JsonArray.of(3),
                "nested", JsonObject.of("a", JsonArray.of(3, 4)));
        blockingJson.jsonSet(key, "$", test);
        assertThat(blockingJson.jsonArrInsert(key, "$..a", 0, 1, 2)).containsExactly(3, 4);
        JsonObject object = blockingJson.jsonGetObject(key);
        assertThat(object.getJsonArray("a")).containsExactly(1, 2, 3);
        assertThat(object.getJsonObject("nested").getJsonArray("a")).containsExactly(1, 2, 3, 4);
    }

    @Test
    void jsonArrInsertWithNull() {
        JsonObject test = JsonObject.of("a", JsonArray.of(1, 2, 3, 2),
                "nested", JsonObject.of("a", 1));
        blockingJson.jsonSet(key, "$", test);
        assertThat(blockingJson.jsonArrInsert(key, "$..a", 0, 1, 2)).containsExactly(6, null);
    }

    @Test
    void jsonArrayLen() {
        JsonObject test = JsonObject.of("a", JsonArray.of(3),
                "nested", JsonObject.of("a", JsonArray.of(3, 4)));
        blockingJson.jsonSet(key, "$", test);
        blockingJson.jsonSet("doc", "$", JsonArray.of("a", "b", "c"));
        blockingJson.jsonSet("doc2", "$", JsonObject.of("a", "b"));

        assertThat(blockingJson.jsonArrLen(key, "$..a")).containsExactly(1, 2);
        assertThat(blockingJson.jsonArrLen("doc")).hasValue(3);
    }

    @Test
    void jsonArrayLenWithNull() {
        JsonObject test = JsonObject.of("a", JsonArray.of(1, 2, 3, 2),
                "nested", JsonObject.of("a", 2));
        blockingJson.jsonSet(key, "$", test);
        assertThat(blockingJson.jsonArrLen(key, "$..a")).containsExactly(4, null);
    }

    @Test
    void jsonArrayPop() {
        JsonObject test = JsonObject.of("a", JsonArray.of(3),
                "nested", JsonObject.of("a", JsonArray.of(3, 4)));
        blockingJson.jsonSet(key, "$", test);

        assertThat(blockingJson.jsonArrPop(key, Integer.class, "$..a", -1)).containsExactly(3, 4);
        assertThat(blockingJson.jsonGetObject(key).getJsonArray("a")).isEmpty();
        assertThat(blockingJson.jsonGetObject(key).getJsonObject("nested").getJsonArray("a")).containsExactly(3);
    }

    @Test
    void jsonArrayPopWithIndex() {
        blockingJson.jsonSet("arr", "$", JsonArray.of(1, 2, 3, 4));
        assertThat(blockingJson.jsonArrPop("arr", Integer.class, "$", 0)).containsExactly(1);
        assertThat(blockingJson.jsonGetArray("arr")).containsExactly(2, 3, 4);
    }

    @Test
    void jsonArrayPopWithIndexAndDefaultPath() {
        blockingJson.jsonSet("arr", "$", JsonArray.of(1, 2, 3, 4));
        assertThat(blockingJson.jsonArrPop("arr", Integer.class, null, 0)).containsExactly(1);
        assertThat(blockingJson.jsonArrPop("arr", Integer.class, null, 1)).containsExactly(3);
        assertThat(blockingJson.jsonGetArray("arr")).containsExactly(2, 4);
    }

    @Test
    void jsonArrayPopNoMatch() {
        blockingJson.jsonSet(key, "$", JsonObject.of("a", JsonArray.of(1, 2)));
        assertThat(blockingJson.jsonArrPop(key, Integer.class, "$..missing", -1)).isEmpty();
    }

    @Test
    void jsonArrayPopWithNull() {
        JsonObject test = JsonObject.of("a", JsonArray.of("foo", "bar"),
                "nested", JsonObject.of("a", 2), "nested2", JsonObject.of("a", new JsonArray()));
        blockingJson.jsonSet(key, "$", test);
        assertThat(blockingJson.jsonArrPop(key, String.class, "$..a")).containsExactly("bar", null, null);
    }

    @Test
    void jsonArrayPopWithDefault() {
        JsonObject test = JsonObject.of("a", JsonArray.of(3),
                "nested", JsonObject.of("a", JsonArray.of(3, 4)));
        blockingJson.jsonSet(key, "$", test);

        assertThat(blockingJson.jsonArrPop(key, Integer.class, "$..a")).containsExactly(3, 4);
        assertThat(blockingJson.jsonGetObject(key).getJsonArray("a")).isEmpty();
        assertThat(blockingJson.jsonGetObject(key).getJsonObject("nested").getJsonArray("a")).containsExactly(3);

        blockingJson.jsonSet("arr", "$", JsonArray.of(1, 2, 3, 4));
        assertThat(blockingJson.jsonArrPop("arr", Integer.class)).isEqualTo(4);

        blockingJson.jsonSet("empty", "$", JsonArray.of());
        assertThat(blockingJson.jsonArrPop("empty", Integer.class)).isNull();
    }

    @Test
    void jsonArrayTrim() {
        JsonObject test = JsonObject.of("a", JsonArray.of(),
                "nested", JsonObject.of("a", JsonArray.of(1, 4)));
        blockingJson.jsonSet(key, "$", test);

        assertThat(blockingJson.jsonArrTrim(key, "$..a", 1, 1)).containsExactly(0, 1);
        assertThat(blockingJson.jsonGetObject(key).getJsonArray("a")).isEmpty();
        assertThat(blockingJson.jsonGetObject(key).getJsonObject("nested").getJsonArray("a")).containsExactly(4);
    }

    @Test
    void jsonArrayTrimWithZeroBounds() {
        blockingJson.jsonSet(key, "$", JsonObject.of("a", JsonArray.of(1, 2, 3, 4)));

        // stop == 0 keeps the first element only; Lettuce's own range arguments would drop the bounds here
        assertThat(blockingJson.jsonArrTrim(key, "$.a", 0, 0)).containsExactly(1);
        assertThat(blockingJson.jsonGetObject(key).getJsonArray("a")).containsExactly(1);

        blockingJson.jsonSet(key, "$", JsonObject.of("a", JsonArray.of(1, 2, 3, 4)));
        // start > stop empties the array
        assertThat(blockingJson.jsonArrTrim(key, "$.a", 2, 0)).containsExactly(0);
        assertThat(blockingJson.jsonGetObject(key).getJsonArray("a")).isEmpty();
    }

    @Test
    void jsonArrayTrimWithNull() {
        JsonObject test = JsonObject.of("a", JsonArray.of(1, 2, 3, 2),
                "nested", JsonObject.of("a", 1));
        blockingJson.jsonSet(key, "$", test);

        assertThat(blockingJson.jsonArrTrim(key, "$..a", 1, 1)).containsExactly(1, null);
        assertThat(blockingJson.jsonGetObject(key).getJsonArray("a")).containsExactly(2);
        assertThat(blockingJson.jsonGetObject(key).getJsonObject("nested").getInteger("a")).isEqualTo(1);
    }

    @Test
    void jsonClearAll() {
        JsonObject test = JsonObject.of("obj", JsonObject.of("a", 1, "b", 2),
                "arr", JsonArray.of(1, 2, 3), "str", "foo", "bool", true,
                "int", 42, "float", 3.14);
        blockingJson.jsonSet(key, "$", test);

        blockingJson.jsonClear(key);
        JsonObject object = blockingJson.jsonGet(key);
        assertThat(object).isEmpty();
    }

    @Test
    void jsonClear() {
        JsonObject test = JsonObject.of("obj", JsonObject.of("a", 1, "b", 2),
                "arr", JsonArray.of(1, 2, 3), "str", "foo", "bool", true,
                "int", 42, "float", 3.14);
        blockingJson.jsonSet(key, "$", test);

        assertThat(blockingJson.jsonClear(key, "$.*")).isEqualTo(4);
        JsonObject object = blockingJson.jsonGet(key);
        assertThat(object.getJsonObject("obj")).isEmpty();
        assertThat(object.getJsonArray("arr")).isEmpty();
        assertThat(object.getString("str")).isEqualTo("foo");
        assertThat(object.getBoolean("bool")).isTrue();
        assertThat(object.getInteger("int")).isEqualTo(0);
        assertThat(object.getDouble("float")).isEqualTo(0.0);
    }

    @Test
    void jsonDel() {
        JsonObject test = JsonObject.of("a", 1, "nested", JsonObject.of("a", 2, "b", 3));
        blockingJson.jsonSet(key, "$", test);

        assertThat(blockingJson.jsonDel(key, "$..a")).isEqualTo(2);
        JsonObject object = blockingJson.jsonGet(key);
        assertThat(object.getString("a")).isNull();
        assertThat(object.getJsonObject("nested")).containsExactly(entry("b", 3));
    }

    @Test
    void jsonDelAll() {
        JsonObject test = JsonObject.of("a", 1, "nested", JsonObject.of("a", 2, "b", 3));
        blockingJson.jsonSet(key, "$", test);

        assertThat(blockingJson.jsonDel(key)).isEqualTo(1);
        JsonObject object = blockingJson.jsonGet(key);
        assertThat(object).isNull();
    }

    @Test
    void mget() {
        JsonObject j1 = JsonObject.of("a", 1, "b", 2, "nested", JsonObject.of("a", 3), "c", null);
        JsonObject j2 = JsonObject.of("a", 4, "b", 5, "nested", JsonObject.of("a", 6), "c", null);

        blockingJson.jsonSet("doc1", j1);
        blockingJson.jsonSet("doc2", j2);

        List<JsonArray> arrays = blockingJson.jsonMget("$..a", "doc1", "doc2");
        assertThat(arrays.get(0)).containsExactly(1, 3);
        assertThat(arrays.get(1)).containsExactly(4, 6);

        arrays = blockingJson.jsonMget("$..d", "doc1", "doc2");
        assertThat(arrays).hasSize(2).allSatisfy(a -> assertThat(a).isEmpty());

        arrays = blockingJson.jsonMget("$..a", "doc1");
        assertThat(arrays.get(0)).containsExactly(1, 3);

        arrays = blockingJson.jsonMget("$..a", "doc1", "missing");
        assertThat(arrays).hasSize(2);
        assertThat(arrays.get(0)).containsExactly(1, 3);
        assertThat(arrays.get(1)).isNull();
    }

    @Test
    void numIncrBy() {
        JsonObject test = JsonObject.of("a", "b", "b",
                JsonArray.of(JsonObject.of("a", 2), JsonObject.of("a", 5), JsonObject.of("a", "c")));
        JsonObject res = JsonObject.of("a", "b", "b",
                JsonArray.of(JsonObject.of("a", 4), JsonObject.of("a", 7), JsonObject.of("a", "c")));
        blockingJson.jsonSet(key, test);
        blockingJson.jsonNumincrby(key, "$.a", 2);
        assertThat(blockingJson.jsonGet(key)).isEqualTo(test);
        blockingJson.jsonNumincrby(key, "$..a", 2);
        assertThat(blockingJson.jsonGet(key)).isEqualTo(res);
    }

    @Test
    void objKeys() {
        JsonObject test = JsonObject.of("a", JsonArray.of(3), "nested", JsonObject.of("a", JsonObject.of("b", 2, "c", 1)));
        blockingJson.jsonSet(key, test);
        assertThat(blockingJson.jsonObjKeys(key, "$..a")).containsExactly(null, List.of("b", "c"));

        assertThat(blockingJson.jsonObjKeys(key)).containsExactly("a", "nested");
        assertThat(blockingJson.jsonObjKeys(key, "$..missing")).containsExactly(Collections.emptyList());
    }

    @Test
    void objLen() {
        JsonObject test = JsonObject.of("a", JsonArray.of(3), "nested", JsonObject.of("a", JsonObject.of("b", 2, "c", 1)));
        blockingJson.jsonSet(key, test);
        blockingJson.jsonSet("empty", new JsonObject());
        blockingJson.jsonSet("arr", new JsonArray());
        assertThat(blockingJson.jsonObjLen(key, "$..a")).containsExactly(null, 2);
        assertThat(blockingJson.jsonObjLen(key)).hasValue(2);
        assertThat(blockingJson.jsonObjLen("empty")).hasValue(0);
        assertThat(blockingJson.jsonObjLen(key, "$..missing")).isEmpty();
    }

    @Test
    void strAppend() {
        JsonObject test = JsonObject.of("a", "foo", "nested", JsonObject.of("a", "hello"), "nested2", JsonObject.of("a", 31));
        blockingJson.jsonSet(key, test);
        blockingJson.jsonSet("empty", new JsonObject());
        blockingJson.jsonSet("str", "hello");
        assertThat(blockingJson.jsonStrAppend(key, "$..a", "baz buzz")).containsExactly(11, 13, null);
        JsonObject object = blockingJson.jsonGet(key);
        assertThat(object.getString("a")).isEqualTo("foobaz buzz");
        assertThat(object.getJsonObject("nested").getString("a")).isEqualTo("hellobaz buzz");

        assertThat(blockingJson.jsonStrAppend("str", null, "-hello")).containsExactly(11);
        assertThat(blockingJson.jsonStrAppend(key, "$..missing", "gaa")).isEmpty();
    }

    @Test
    void strLen() {
        JsonObject test = JsonObject.of("a", "foo", "nested", JsonObject.of("a", "hello"), "nested2", JsonObject.of("a", 31));
        blockingJson.jsonSet(key, test);
        blockingJson.jsonSet("empty", new JsonObject());
        blockingJson.jsonSet("str", "hello");
        assertThat(blockingJson.jsonStrLen(key, "$..a")).containsExactly(3, 5, null);

        assertThat(blockingJson.jsonStrLen("str", null)).containsExactly(5);
        assertThat(blockingJson.jsonStrLen(key, "$..missing")).isEmpty();
    }

    @Test
    void toggle() {
        JsonObject test = JsonObject.of("a", true, "nested", JsonObject.of("a", false), "nested2", JsonObject.of("a", "true"));
        blockingJson.jsonSet(key, test);
        blockingJson.jsonSet("empty", new JsonObject());
        blockingJson.jsonSet("bool", false);
        assertThat(blockingJson.jsonToggle(key, "$..a")).containsExactly(false, true, null);
        JsonObject object = blockingJson.jsonGet(key);
        assertThat(object.getBoolean("a")).isFalse();
        assertThat(object.getJsonObject("nested").getBoolean("a")).isTrue();
        assertThat(object.getJsonObject("nested2").getString("a")).isEqualTo("true");

        assertThat(blockingJson.jsonToggle("bool", "$")).containsExactly(true);
        assertThat(blockingJson.jsonToggle("empty", "$")).hasSize(1).allSatisfy(b -> assertThat(b).isNull());
        assertThat(blockingJson.jsonToggle(key, "$..missing")).isEmpty();
    }

    @Test
    void type() {
        JsonObject test = JsonObject.of("a", 2, "nested", JsonObject.of("a", true), "foo", "bar", "arr", JsonArray.of(1, 2, 3),
                "next", JsonObject.of("a", 23.5));
        blockingJson.jsonSet(key, test);
        blockingJson.jsonSet("empty", new JsonObject());
        assertThat(blockingJson.jsonType(key, "$..foo")).containsExactly("string");
        assertThat(blockingJson.jsonType(key, "$..a")).containsExactly("integer", "boolean", "number");
        assertThat(blockingJson.jsonType(key, "$..missing")).isEmpty();
        assertThat(blockingJson.jsonType(key, "$.arr")).containsExactly("array");
        assertThat(blockingJson.jsonType(key, "$.arr[0]")).containsExactly("integer");
        assertThat(blockingJson.jsonType("empty", "$")).containsExactly("object");
        assertThat(blockingJson.jsonType("empty", "$.a")).isEmpty();
        assertThat(blockingJson.jsonType(key, null)).containsExactly("object");
    }

    @Test
    void testJsonWithTypeReference() {
        var json = blockingDs.json(new TypeReference<List<String>>() {
            // Empty on purpose
        });

        var key = List.of("a", "b", "c");

        json.jsonSet(key, "$",
                new JsonObject().put("a", 2).put("b", 3).put("nested", new JsonObject().put("a", 4).put("b", null)));
        assertThat(json.jsonGet(key, "$..b")).containsExactly(3, null);
        JsonObject object = json.jsonGet(key, "..a", "$..b");
        assertThat(object).hasSize(2);
        assertThat(object.getJsonArray("..a")).containsExactly(2, 4);
        assertThat(object.getJsonArray("$..b")).containsExactly(3, null);
    }

}
