package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;

import io.quarkus.redis.datasource.json.JsonCommands;
import io.quarkus.redis.datasource.json.JsonSetArgs;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@RequiresCommand("json.get")
public class JsonCommandsTest extends DatasourceTestBase {

    private RedisDataSource ds;

    private JsonCommands<String> json;

    @BeforeEach
    void initialize() {
        ds = new BlockingRedisDataSourceImpl(vertx, redis, api, Duration.ofSeconds(1));
        json = ds.json();
    }

    @AfterEach
    public void clear() {
        ds.flushall();
    }

    @Test
    void getDataSource() {
        assertThat(ds).isEqualTo(json.getDataSource());
    }

    @Test
    public void testJson() {
        json.jsonSet("doc", "$",
                new JsonObject().put("a", 2).put("b", 3).put("nested", new JsonObject().put("a", 4).put("b", null)));
        assertThat(json.jsonGet("doc", "$..b")).containsExactly(3, null);
        JsonObject object = json.jsonGet("doc", "..a", "$..b");
        assertThat(object).hasSize(2);
        assertThat(object.getJsonArray("..a")).containsExactly(2, 4);
        assertThat(object.getJsonArray("$..b")).containsExactly(3, null);
    }

    @Test
    public void jsonSetRoot() {
        json.jsonSet("animal", "$", "dog");
        assertThat(json.jsonGet("animal", "$")).containsExactly("dog");
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
    public void jsonSet() {
        Person p = new Person();
        p.name = "luke";
        p.age = 20;

        json.jsonSet("luke", "$", p);
        assertThat(json.jsonGet("luke", Person.class)).isEqualTo(p);

        Person p2 = new Person();
        p2.name = "leia";
        p2.age = 20;
        json.jsonSet("luke", "$.sister", p2);
        json.jsonSet("luke", "$.friends", new JsonArray().add("Obiwan").add("Han"), new JsonSetArgs().nx());
        json.jsonSet("luke", "$.enemies", new JsonArray().add("Darth Sidious"), new JsonSetArgs().nx());
        json.jsonSet("luke", "$.weapons", new JsonArray().add("Light Saber"));
        json.jsonSet("luke", "$.father", new JsonObject().put("name", "Darth Vader").put("otherName", "Anakin"));
        json.jsonSet("luke", "$.sister.father", new JsonObject().put("name", "Darth Vader").put("otherName", "Anakin"));

        JsonObject luke = json.jsonGetObject("luke");
        assertThat(luke.getString("name")).isEqualTo("luke");
        assertThat(luke.getInteger("age")).isEqualTo(20);
        assertThat(luke.getJsonArray("enemies")).containsExactly("Darth Sidious");
        assertThat(luke.getJsonArray("weapons")).containsExactly("Light Saber");
        assertThat(luke.getJsonObject("sister").getInteger("age")).isEqualTo(20);
        assertThat(luke.getJsonObject("sister").getString("name")).isEqualTo("leia");
        assertThat(luke.getJsonObject("sister").getJsonObject("father").getString("name")).isEqualTo("Darth Vader");
        assertThat(luke.getJsonArray("friends")).containsExactly("Obiwan", "Han");
        assertThat(luke.getJsonObject("father").getString("name")).isEqualTo("Darth Vader");

        json.jsonSet("luke", "$", p, new JsonSetArgs().nx());

        luke = json.jsonGetObject("luke");
        assertThat(luke.getString("name")).isEqualTo("luke");
        assertThat(luke.getInteger("age")).isEqualTo(20);
        assertThat(luke.getJsonObject("sister").getInteger("age")).isEqualTo(20);
        assertThat(luke.getJsonObject("sister").getString("name")).isEqualTo("leia");
        assertThat(luke.getJsonObject("sister").getJsonObject("father").getString("name")).isEqualTo("Darth Vader");
        assertThat(luke.getJsonArray("friends")).containsExactly("Obiwan", "Han");
        assertThat(luke.getJsonObject("father").getString("name")).isEqualTo("Darth Vader");

        json.jsonSet("luke", "$", p, new JsonSetArgs().xx());

        luke = json.jsonGetObject("luke");
        assertThat(luke.getString("name")).isEqualTo("luke");
        assertThat(luke.getInteger("age")).isEqualTo(20);
        assertThat(luke.getJsonObject("sister")).isNull();
    }

    @Test
    public void jsonGetArray() {
        json.jsonSet("array", "$", List.of("a", "b", "c"));
        assertThat(json.jsonGetArray("array")).containsExactly("a", "b", "c");
    }

    @Test
    public void jsonSetNull() {
        json.jsonSet("null", "$", (Object) null);
        assertThatThrownBy(() -> json.jsonSet("null-json", "$", (JsonArray) null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> json.jsonSet("null-json", "$", (JsonObject) null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(json.jsonGet("null")).isNull();
        assertThat(json.jsonGet("null", String.class)).isNull();

        assertThat(json.jsonGet("missing", String.class)).isNull();
        assertThat(json.jsonGet("missing")).isNull();
    }

    @Test
    public void testJsonGet() {
        Person p = new Person();
        p.name = "luke";
        p.age = 20;

        json.jsonSet("luke", "$", p);
        assertThat(json.jsonGet("luke", Person.class)).isEqualTo(p);

        Person p2 = new Person();
        p2.name = "leia";
        p2.age = 20;
        json.jsonSet("luke", "$.sister", p2);
        json.jsonSet("luke", "$.friends", new JsonArray().add("Obiwan").add("Han"), new JsonSetArgs().nx());
        json.jsonSet("luke", "$.enemies", new JsonArray().add("Darth Sidious"), new JsonSetArgs().nx());
        json.jsonSet("luke", "$.weapons", new JsonArray().add("Light Saber"));
        json.jsonSet("luke", "$.father", new JsonObject().put("name", "Darth Vader").put("otherName", "Anakin"));
        json.jsonSet("luke", "$.sister.father", new JsonObject().put("name", "Darth Vader").put("otherName", "Anakin"));

        assertThat(json.jsonGet("luke", "$")).hasSize(1);
        assertThat(json.jsonGet("luke", "$.missing")).hasSize(0);
        assertThat(json.jsonGet("luke").getString("name")).isEqualTo("luke");
        assertThat(json.jsonGet("luke", "$.friends[0]").getString(0)).isEqualTo("Obiwan");
        assertThat(json.jsonGet("luke", "$.friends[2]")).hasSize(0);
        JsonObject query = json.jsonGet("luke", "$.friends", "..father");
        assertThat(query).hasSize(2);
        assertThat(query.getJsonArray("$.friends").getJsonArray(0)).containsExactly("Obiwan", "Han");
        assertThat(query.getJsonArray("..father").getJsonObject(0).getString("name")).isEqualTo("Darth Vader");
        assertThat(query.getJsonArray("..father").getJsonObject(1).getString("name")).isEqualTo("Darth Vader");

        query = json.jsonGet("luke", "$.friends", ".missing", "..father");
        assertThat(query).hasSize(3);
        assertThat(query.getJsonArray(".missing")).isEmpty();
    }

    @Test
    void jsonArrAppend() {
        JsonObject test = JsonObject.of("a", 10, "arr", JsonArray.of(1, 2));
        JsonArray arr = JsonArray.of("a", "b", "c");
        json.jsonSet(key, "$", test);
        json.jsonSet("arr", "$", arr);
        json.jsonSet("obj", "$", JsonObject.of("name", "clement"));

        List<Integer> list = json.jsonArrAppend(key, "arr", new io.quarkus.redis.datasource.Person("luke", "skywalker"));
        assertThat(list).containsExactly(3); // 1, 2, luke...
        list = json.jsonArrAppend("arr", "$", new io.quarkus.redis.datasource.Person("luke", "skywalker"));
        assertThat(list).containsExactly(4); // a, b, c, luke...
        assertThatThrownBy(() -> json.jsonArrAppend("obj", ".name", "a", "b"))
                .hasMessageContaining(".name");

        assertThatThrownBy(() -> json.jsonArrAppend("missing", ".name", "a", "b"))
                .hasMessageContaining("key");
    }

    @Test
    void jsonArrIndex() {
        JsonObject test = JsonObject.of("a", JsonArray.of(1, 2, 3, 2),
                "nested", JsonObject.of("a", JsonArray.of(3, 4)));
        json.jsonSet(key, "$", test);
        assertThat(json.jsonArrIndex(key, "$..a", 2)).containsExactly(1, -1);
        assertThat(json.jsonArrIndex(key, "$..a", 2, 0, 10)).containsExactly(1, -1);

        assertThatThrownBy(() -> json.jsonArrIndex(key, ".name", 2))
                .hasMessageContaining(".name");

        assertThatThrownBy(() -> json.jsonArrIndex("missing", ".name", "a"))
                .hasMessageContaining(".name");

        test = JsonObject.of("a", JsonArray.of(1, 2, 3, 2),
                "nested", JsonObject.of("a", false));
        json.jsonSet(key, "$", test);
        assertThat(json.jsonArrIndex(key, "$..a", 2)).containsExactly(1, null);
    }

    @Test
    void jsonArrInsert() {
        JsonObject test = JsonObject.of("a", JsonArray.of(3),
                "nested", JsonObject.of("a", JsonArray.of(3, 4)));
        json.jsonSet(key, "$", test);
        assertThat(json.jsonArrInsert(key, "$..a", 0, 1, 2)).containsExactly(3, 4);
        JsonObject object = json.jsonGetObject(key);
        assertThat(object.getJsonArray("a")).containsExactly(1, 2, 3);
        assertThat(object.getJsonObject("nested").getJsonArray("a")).containsExactly(1, 2, 3, 4);
    }

    @Test
    void jsonArrInsertWithNull() {
        JsonObject test = JsonObject.of("a", JsonArray.of(1, 2, 3, 2),
                "nested", JsonObject.of("a", 1));
        json.jsonSet(key, "$", test);
        assertThat(json.jsonArrInsert(key, "$..a", 0, 1, 2)).containsExactly(6, null);
    }

    @Test
    void jsonArrayLen() {
        JsonObject test = JsonObject.of("a", JsonArray.of(3),
                "nested", JsonObject.of("a", JsonArray.of(3, 4)));
        json.jsonSet(key, "$", test);
        json.jsonSet("doc", "$", JsonArray.of("a", "b", "c"));
        json.jsonSet("doc2", "$", JsonObject.of("a", "b"));

        assertThat(json.jsonArrLen(key, "$..a")).containsExactly(1, 2);
        assertThat(json.jsonArrLen("doc")).hasValue(3);
    }

    @Test
    void jsonArrayLenWithNull() {
        JsonObject test = JsonObject.of("a", JsonArray.of(1, 2, 3, 2),
                "nested", JsonObject.of("a", 2));
        json.jsonSet(key, "$", test);
        assertThat(json.jsonArrLen(key, "$..a")).containsExactly(4, null);
    }

    @Test
    void jsonArrayPop() {
        JsonObject test = JsonObject.of("a", JsonArray.of(3),
                "nested", JsonObject.of("a", JsonArray.of(3, 4)));
        json.jsonSet(key, "$", test);

        assertThat(json.jsonArrPop(key, Integer.class, "$..a", -1)).containsExactly(3, 4);
        assertThat(json.jsonGetObject(key).getJsonArray("a")).isEmpty();
        assertThat(json.jsonGetObject(key).getJsonObject("nested").getJsonArray("a")).containsExactly(3);
    }

    @Test
    void jsonArrayPopWithNull() {
        JsonObject test = JsonObject.of("a", JsonArray.of("foo", "bar"),
                "nested", JsonObject.of("a", 2), "nested2", JsonObject.of("a", new JsonArray()));
        json.jsonSet(key, "$", test);
        assertThat(json.jsonArrPop(key, String.class, "$..a")).containsExactly("bar", null, null);
    }

    @Test
    void jsonArrayPopWithDefault() {
        JsonObject test = JsonObject.of("a", JsonArray.of(3),
                "nested", JsonObject.of("a", JsonArray.of(3, 4)));
        json.jsonSet(key, "$", test);

        assertThat(json.jsonArrPop(key, Integer.class, "$..a")).containsExactly(3, 4);
        assertThat(json.jsonGetObject(key).getJsonArray("a")).isEmpty();
        assertThat(json.jsonGetObject(key).getJsonObject("nested").getJsonArray("a")).containsExactly(3);

        json.jsonSet("arr", "$", JsonArray.of(1, 2, 3, 4));
        assertThat(json.jsonArrPop("arr", Integer.class)).isEqualTo(4);

        json.jsonSet("empty", "$", JsonArray.of());
        assertThat(json.jsonArrPop("empty", Integer.class)).isNull();
    }

    @Test
    void jsonArrayTrim() {
        JsonObject test = JsonObject.of("a", JsonArray.of(),
                "nested", JsonObject.of("a", JsonArray.of(1, 4)));
        json.jsonSet(key, "$", test);

        assertThat(json.jsonArrTrim(key, "$..a", 1, 1)).containsExactly(0, 1);
        assertThat(json.jsonGetObject(key).getJsonArray("a")).isEmpty();
        assertThat(json.jsonGetObject(key).getJsonObject("nested").getJsonArray("a")).containsExactly(4);
    }

    @Test
    void jsonArrayTrimWithNull() {
        JsonObject test = JsonObject.of("a", JsonArray.of(1, 2, 3, 2),
                "nested", JsonObject.of("a", 1));
        json.jsonSet(key, "$", test);

        assertThat(json.jsonArrTrim(key, "$..a", 1, 1)).containsExactly(1, null);
        assertThat(json.jsonGetObject(key).getJsonArray("a")).containsExactly(2);
        assertThat(json.jsonGetObject(key).getJsonObject("nested").getInteger("a")).isEqualTo(1);
    }

    @Test
    void jsonClearAll() {
        JsonObject test = JsonObject.of("obj", JsonObject.of("a", 1, "b", 2),
                "arr", JsonArray.of(1, 2, 3), "str", "foo", "bool", true,
                "int", 42, "float", 3.14);
        json.jsonSet(key, "$", test);

        json.jsonClear(key);
        JsonObject object = json.jsonGet(key);
        assertThat(object).isEmpty();
    }

    @Test
    void jsonClear() {
        JsonObject test = JsonObject.of("obj", JsonObject.of("a", 1, "b", 2),
                "arr", JsonArray.of(1, 2, 3), "str", "foo", "bool", true,
                "int", 42, "float", 3.14);
        json.jsonSet(key, "$", test);

        json.jsonClear(key, "$.*");
        JsonObject object = json.jsonGet(key);
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
        json.jsonSet(key, "$", test);

        json.jsonDel(key, "$..a");
        JsonObject object = json.jsonGet(key);
        assertThat(object.getString("a")).isNull();
        assertThat(object.getJsonObject("nested")).containsExactly(entry("b", 3));
    }

    @Test
    void jsonDelAll() {
        JsonObject test = JsonObject.of("a", 1, "nested", JsonObject.of("a", 2, "b", 3));
        json.jsonSet(key, "$", test);

        json.jsonDel(key);
        JsonObject object = json.jsonGet(key);
        assertThat(object).isNull();
    }

    @Test
    void mget() {
        // redis> JSON.SET doc1 $ '{"a":1, "b": 2, "nested": {"a": 3}, "c": null}'
        //OK
        //redis> JSON.SET doc2 $ '{"a":4, "b": 5, "nested": {"a": 6}, "c": null}'
        //OK
        //redis> JSON.MGET doc1 doc2 $..a
        //1) "[1,3]"
        //2) "[4,6]"

        JsonObject j1 = JsonObject.of("a", 1, "b", 2, "nested", JsonObject.of("a", 3), "c", null);
        JsonObject j2 = JsonObject.of("a", 4, "b", 5, "nested", JsonObject.of("a", 6), "c", null);

        json.jsonSet("doc1", j1);
        json.jsonSet("doc2", j2);

        List<JsonArray> arrays = json.jsonMget("$..a", "doc1", "doc2");
        assertThat(arrays.get(0)).containsExactly(1, 3);
        assertThat(arrays.get(1)).containsExactly(4, 6);

        arrays = json.jsonMget("$..d", "doc1", "doc2");
        assertThat(arrays).hasSize(2).allSatisfy(a -> assertThat(a).isEmpty());

        arrays = json.jsonMget("$..a", "doc1");
        assertThat(arrays.get(0)).containsExactly(1, 3);
    }

    @Test
    void numIncrBy() {
        JsonObject test = JsonObject.of("a", "b", "b",
                JsonArray.of(JsonObject.of("a", 2), JsonObject.of("a", 5), JsonObject.of("a", "c")));
        JsonObject res = JsonObject.of("a", "b", "b",
                JsonArray.of(JsonObject.of("a", 4), JsonObject.of("a", 7), JsonObject.of("a", "c")));
        json.jsonSet(key, test);
        json.jsonNumincrby(key, "$.a", 2);
        assertThat(json.jsonGet(key)).isEqualTo(test);
        json.jsonNumincrby(key, "$..a", 2);
        assertThat(json.jsonGet(key)).isEqualTo(res);
    }

    @Test
    void objKeys() {
        JsonObject test = JsonObject.of("a", JsonArray.of(3), "nested", JsonObject.of("a", JsonObject.of("b", 2, "c", 1)));
        json.jsonSet(key, test);
        assertThat(json.jsonObjKeys(key, "$..a")).containsExactly(null, List.of("b", "c"));

        assertThat(json.jsonObjKeys(key)).containsExactly("a", "nested");
        assertThat(json.jsonObjKeys(key, "$..missing")).containsExactly(Collections.emptyList());
    }

    @Test
    void objLen() {
        JsonObject test = JsonObject.of("a", JsonArray.of(3), "nested", JsonObject.of("a", JsonObject.of("b", 2, "c", 1)));
        json.jsonSet(key, test);
        json.jsonSet("empty", new JsonObject());
        json.jsonSet("arr", new JsonArray());
        assertThat(json.jsonObjLen(key, "$..a")).containsExactly(null, 2);
        assertThat(json.jsonObjLen(key)).hasValue(2);
        assertThat(json.jsonObjLen("empty")).hasValue(0);
        assertThat(json.jsonObjLen(key, "$..missing")).isEmpty();
    }

    @Test
    void strAppend() {
        JsonObject test = JsonObject.of("a", "foo", "nested", JsonObject.of("a", "hello"), "nested2", JsonObject.of("a", 31));
        json.jsonSet(key, test);
        json.jsonSet("empty", new JsonObject());
        json.jsonSet("str", "hello");
        assertThat(json.jsonStrAppend(key, "$..a", "baz buzz")).containsExactly(11, 13, null);
        JsonObject object = json.jsonGet(key);
        assertThat(object.getString("a")).isEqualTo("foobaz buzz");
        assertThat(object.getJsonObject("nested").getString("a")).isEqualTo("hellobaz buzz");

        assertThat(json.jsonStrAppend("str", null, "-hello")).containsExactly(11);
        assertThat(json.jsonStrAppend(key, "$..missing", "gaa")).isEmpty();
    }

    @Test
    void strLen() {
        JsonObject test = JsonObject.of("a", "foo", "nested", JsonObject.of("a", "hello"), "nested2", JsonObject.of("a", 31));
        json.jsonSet(key, test);
        json.jsonSet("empty", new JsonObject());
        json.jsonSet("str", "hello");
        assertThat(json.jsonStrLen(key, "$..a")).containsExactly(3, 5, null);

        assertThat(json.jsonStrLen("str", null)).containsExactly(5);
        assertThat(json.jsonStrLen(key, "$..missing")).isEmpty();
    }

    @Test
    void toggle() {
        JsonObject test = JsonObject.of("a", true, "nested", JsonObject.of("a", false), "nested2", JsonObject.of("a", "true"));
        json.jsonSet(key, test);
        json.jsonSet("empty", new JsonObject());
        json.jsonSet("bool", false);
        assertThat(json.jsonToggle(key, "$..a")).containsExactly(false, true, null);
        JsonObject object = json.jsonGet(key);
        assertThat(object.getBoolean("a")).isFalse();
        assertThat(object.getJsonObject("nested").getBoolean("a")).isTrue();
        assertThat(object.getJsonObject("nested2").getString("a")).isEqualTo("true");

        assertThat(json.jsonToggle("bool", "$")).containsExactly(true);
        assertThat(json.jsonToggle("empty", "$")).hasSize(1).allSatisfy(b -> assertThat(b).isNull());
        assertThat(json.jsonToggle(key, "$..missing")).isEmpty();
    }

    @Test
    void type() {
        JsonObject test = JsonObject.of("a", 2, "nested", JsonObject.of("a", true), "foo", "bar", "arr", JsonArray.of(1, 2, 3),
                "next", JsonObject.of("a", 23.5));
        json.jsonSet(key, test);
        json.jsonSet("empty", new JsonObject());
        assertThat(json.jsonType(key, "$..foo")).containsExactly("string");
        assertThat(json.jsonType(key, "$..a")).containsExactly("integer", "boolean", "number");
        assertThat(json.jsonType(key, "$..missing")).isEmpty();
        assertThat(json.jsonType(key, "$.arr")).containsExactly("array");
        assertThat(json.jsonType(key, "$.arr[0]")).containsExactly("integer");
        assertThat(json.jsonType("empty", "$")).containsExactly("object");
        assertThat(json.jsonType("empty", "$.a")).isEmpty();
    }

    @Test
    public void testJsonWithTypeReference() {
        var json = ds.json(new TypeReference<List<String>>() {
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
