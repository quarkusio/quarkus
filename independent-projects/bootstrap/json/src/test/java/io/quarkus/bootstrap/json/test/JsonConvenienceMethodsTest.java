package io.quarkus.bootstrap.json.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.json.JsonArray;
import io.quarkus.bootstrap.json.JsonBoolean;
import io.quarkus.bootstrap.json.JsonDouble;
import io.quarkus.bootstrap.json.JsonInteger;
import io.quarkus.bootstrap.json.JsonMember;
import io.quarkus.bootstrap.json.JsonNull;
import io.quarkus.bootstrap.json.JsonObject;
import io.quarkus.bootstrap.json.JsonReader;
import io.quarkus.bootstrap.json.JsonString;

class JsonConvenienceMethodsTest {

    @Test
    void unwrapString() {
        JsonObject obj = JsonReader.of("{\"name\":\"Alice\"}").read();
        assertEquals("Alice", obj.unwrapString("name"));
    }

    @Test
    void unwrapStringReturnsNullWhenAbsent() {
        JsonObject obj = JsonReader.of("{\"name\":\"Alice\"}").read();
        assertNull(obj.unwrapString("missing"));
    }

    @Test
    void unwrapStringThrowsOnTypeMismatch() {
        JsonObject obj = JsonReader.of("{\"age\":30}").read();
        var e = assertThrows(IllegalArgumentException.class, () -> obj.unwrapString("age"));
        assertTrue(e.getMessage().contains("age"));
        assertTrue(e.getMessage().contains("string"));
    }

    @Test
    void unwrapBoolean() {
        JsonObject obj = JsonReader.of("{\"active\":true,\"deleted\":false}").read();
        assertTrue(obj.unwrapBoolean("active"));
        assertFalse(obj.unwrapBoolean("deleted"));
    }

    @Test
    void unwrapBooleanReturnsFalseWhenAbsent() {
        JsonObject obj = JsonReader.of("{}").read();
        assertFalse(obj.unwrapBoolean("missing"));
    }

    @Test
    void unwrapBooleanThrowsOnTypeMismatch() {
        JsonObject obj = JsonReader.of("{\"flag\":\"yes\"}").read();
        var e = assertThrows(IllegalArgumentException.class, () -> obj.unwrapBoolean("flag"));
        assertTrue(e.getMessage().contains("flag"));
        assertTrue(e.getMessage().contains("boolean"));
    }

    @Test
    void unwrapInt() {
        JsonObject obj = JsonReader.of("{\"count\":42}").read();
        assertEquals(42, obj.unwrapInt("count", -1));
    }

    @Test
    void unwrapIntReturnsDefaultWhenAbsent() {
        JsonObject obj = JsonReader.of("{}").read();
        assertEquals(-1, obj.unwrapInt("missing", -1));
    }

    @Test
    void unwrapIntThrowsOnTypeMismatch() {
        JsonObject obj = JsonReader.of("{\"count\":\"not a number\"}").read();
        var e = assertThrows(IllegalArgumentException.class, () -> obj.unwrapInt("count", 0));
        assertTrue(e.getMessage().contains("count"));
        assertTrue(e.getMessage().contains("integer"));
    }

    @Test
    void unwrapLong() {
        JsonObject obj = JsonReader.of("{\"big\":9999999999}").read();
        assertEquals(9999999999L, obj.unwrapLong("big", 0L));
        assertEquals(0L, obj.unwrapLong("missing", 0L));
    }

    @Test
    void unwrapLongThrowsOnTypeMismatch() {
        JsonObject obj = JsonReader.of("{\"big\":true}").read();
        assertThrows(IllegalArgumentException.class, () -> obj.unwrapLong("big", 0L));
    }

    @Test
    void unwrapDouble() {
        JsonObject obj = JsonReader.of("{\"ratio\":3.14,\"count\":5}").read();
        assertEquals(3.14, obj.unwrapDouble("ratio", 0.0), 0.001);
        assertEquals(5.0, obj.unwrapDouble("count", 0.0), 0.001);
        assertEquals(0.0, obj.unwrapDouble("missing", 0.0), 0.001);
    }

    @Test
    void unwrapDoubleThrowsOnTypeMismatch() {
        JsonObject obj = JsonReader.of("{\"ratio\":\"high\"}").read();
        assertThrows(IllegalArgumentException.class, () -> obj.unwrapDouble("ratio", 0.0));
    }

    @Test
    void unwrapObject() {
        JsonObject obj = JsonReader.of("{\"inner\":{\"x\":1}}").read();
        JsonObject inner = obj.unwrapObject("inner");
        assertEquals(1, inner.unwrapInt("x", 0));
    }

    @Test
    void unwrapObjectReturnsNullWhenAbsent() {
        JsonObject obj = JsonReader.of("{}").read();
        assertNull(obj.unwrapObject("missing"));
    }

    @Test
    void unwrapObjectThrowsOnTypeMismatch() {
        JsonObject obj = JsonReader.of("{\"inner\":\"not an object\"}").read();
        assertThrows(IllegalArgumentException.class, () -> obj.unwrapObject("inner"));
    }

    @Test
    void unwrapArray() {
        JsonObject obj = JsonReader.of("{\"items\":[1,2,3]}").read();
        JsonArray items = obj.unwrapArray("items");
        assertEquals(3, items.size());
    }

    @Test
    void unwrapArrayReturnsNullWhenAbsent() {
        JsonObject obj = JsonReader.of("{}").read();
        assertNull(obj.unwrapArray("missing"));
    }

    @Test
    void unwrapArrayThrowsOnTypeMismatch() {
        JsonObject obj = JsonReader.of("{\"items\":42}").read();
        assertThrows(IllegalArgumentException.class, () -> obj.unwrapArray("items"));
    }

    @Test
    void mapArray() {
        JsonObject obj = JsonReader.of("{\"people\":[{\"name\":\"A\"},{\"name\":\"B\"}]}").read();
        List<String> names = obj.mapArray("people", o -> o.unwrapString("name"));
        assertEquals(List.of("A", "B"), names);
    }

    @Test
    void mapArrayReturnsEmptyWhenAbsent() {
        JsonObject obj = JsonReader.of("{}").read();
        assertEquals(List.of(), obj.mapArray("missing", o -> o.unwrapString("name")));
    }

    @Test
    void unwrapStringList() {
        JsonObject obj = JsonReader.of("{\"tags\":[\"a\",\"b\",\"c\"]}").read();
        assertEquals(List.of("a", "b", "c"), obj.unwrapStringList("tags"));
    }

    @Test
    void unwrapStringListReturnsEmptyWhenAbsent() {
        JsonObject obj = JsonReader.of("{}").read();
        assertEquals(List.of(), obj.unwrapStringList("missing"));
    }

    @Test
    void unwrapStringListWithMixedTypes() {
        JsonObject obj = JsonReader.of("{\"vals\":[\"a\",42,true]}").read();
        List<String> vals = obj.unwrapStringList("vals");
        assertEquals(List.of("a", "42", "true"), vals);
    }

    @Test
    void mapArrayThrowsOnTypeMismatch() {
        JsonObject obj = JsonReader.of("{\"people\":\"not an array\"}").read();
        assertThrows(IllegalArgumentException.class, () -> obj.mapArray("people", o -> o.unwrapString("name")));
    }

    @Test
    void unwrapStringListThrowsOnTypeMismatch() {
        JsonObject obj = JsonReader.of("{\"tags\":42}").read();
        assertThrows(IllegalArgumentException.class, () -> obj.unwrapStringList("tags"));
    }

    @Test
    void toMap() {
        JsonObject obj = JsonReader.of(
                "{\"name\":\"test\",\"count\":5,\"active\":true,\"nested\":{\"x\":\"y\"},\"items\":[1,\"two\"]}").read();
        Map<String, Object> map = obj.toMap();
        assertEquals("test", map.get("name"));
        assertEquals(5, map.get("count"));
        assertEquals(true, map.get("active"));

        @SuppressWarnings("unchecked")
        Map<String, Object> nested = (Map<String, Object>) map.get("nested");
        assertEquals("y", nested.get("x"));

        @SuppressWarnings("unchecked")
        List<Object> items = (List<Object>) map.get("items");
        assertEquals(1, items.get(0));
        assertEquals("two", items.get(1));
    }

    @Test
    void toMapWithNull() {
        JsonObject obj = JsonReader.of("{\"key\":null}").read();
        Map<String, Object> map = obj.toMap();
        assertTrue(map.containsKey("key"));
        assertNull(map.get("key"));
    }

    @Test
    void arrayMap() {
        JsonArray arr = JsonReader.of("[{\"id\":1},{\"id\":2},{\"id\":3}]").read();
        List<Integer> ids = arr.map(v -> ((JsonObject) v).unwrapInt("id", 0));
        assertEquals(List.of(1, 2, 3), ids);
    }

    @Test
    void arrayToStringList() {
        JsonArray arr = JsonReader.of("[\"a\",\"b\",\"c\"]").read();
        assertEquals(List.of("a", "b", "c"), arr.toStringList());
    }

    @Test
    void arrayToList() {
        JsonArray arr = JsonReader.of("[1,\"two\",true,null,{\"k\":\"v\"},[3]]").read();
        List<Object> list = arr.toList();
        assertEquals(1, list.get(0));
        assertEquals("two", list.get(1));
        assertEquals(true, list.get(2));
        assertNull(list.get(3));

        @SuppressWarnings("unchecked")
        Map<String, Object> obj = (Map<String, Object>) list.get(4);
        assertEquals("v", obj.get("k"));

        @SuppressWarnings("unchecked")
        List<Object> nested = (List<Object>) list.get(5);
        assertEquals(List.of(3), nested);
    }

    // --- JsonValue.unwrap() direct tests ---

    @Test
    void unwrapJsonString() {
        assertEquals("hello", new JsonString("hello").unwrap());
    }

    @Test
    void unwrapJsonIntegerFitsInt() {
        Object result = new JsonInteger(42).unwrap();
        assertEquals(Integer.class, result.getClass());
        assertEquals(42, result);
    }

    @Test
    void unwrapJsonIntegerRequiresLong() {
        Object result = new JsonInteger(9999999999L).unwrap();
        assertEquals(Long.class, result.getClass());
        assertEquals(9999999999L, result);
    }

    @Test
    void unwrapJsonDouble() {
        assertEquals(3.14, new JsonDouble(3.14).unwrap());
    }

    @Test
    void unwrapJsonBooleanTrue() {
        assertEquals(true, JsonBoolean.TRUE.unwrap());
    }

    @Test
    void unwrapJsonBooleanFalse() {
        assertEquals(false, JsonBoolean.FALSE.unwrap());
    }

    @Test
    void unwrapJsonNull() {
        assertNull(JsonNull.INSTANCE.unwrap());
    }

    @Test
    void unwrapJsonObject() {
        JsonObject obj = JsonReader.of("{\"a\":1}").read();
        Object result = obj.unwrap();
        assertTrue(result instanceof Map);
        assertEquals(1, ((Map<?, ?>) result).get("a"));
    }

    @Test
    void unwrapJsonArray() {
        JsonArray arr = JsonReader.of("[1,2]").read();
        Object result = arr.unwrap();
        assertTrue(result instanceof List);
        assertEquals(List.of(1, 2), result);
    }

    @Test
    void unwrapJsonMember() {
        JsonMember member = new JsonMember("key", new JsonString("value"));
        assertEquals("value", member.unwrap());
    }

    // --- Ordering and toString tests ---

    @Test
    void toMapPreservesInsertionOrder() {
        JsonObject obj = JsonReader.of("{\"z\":1,\"a\":2,\"m\":3}").read();
        Map<String, Object> map = obj.toMap();
        List<String> keys = new ArrayList<>(map.keySet());
        assertEquals(List.of("z", "a", "m"), keys);
    }

    @Test
    void booleanToString() {
        assertEquals("true", JsonBoolean.TRUE.toString());
        assertEquals("false", JsonBoolean.FALSE.toString());
    }
}
