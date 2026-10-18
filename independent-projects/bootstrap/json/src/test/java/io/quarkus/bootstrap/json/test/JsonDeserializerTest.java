package io.quarkus.bootstrap.json.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.json.JsonArray;
import io.quarkus.bootstrap.json.JsonBoolean;
import io.quarkus.bootstrap.json.JsonDouble;
import io.quarkus.bootstrap.json.JsonInteger;
import io.quarkus.bootstrap.json.JsonNull;
import io.quarkus.bootstrap.json.JsonObject;
import io.quarkus.bootstrap.json.JsonReader;
import io.quarkus.bootstrap.json.JsonString;

class JsonDeserializerTest {

    @Test
    void testSimpleObject() {
        JsonObject obj = JsonReader.of("{\"name\":\"John\",\"age\":30}").read();
        assertNotNull(obj);
        assertEquals("John", ((JsonString) obj.get("name")).value());
        assertEquals(30L, ((JsonInteger) obj.get("age")).longValue());
    }

    @Test
    void testSimpleArray() {
        JsonArray arr = JsonReader.of("[\"apple\",\"banana\",\"cherry\"]").read();
        assertNotNull(arr);
        assertEquals(3, arr.size());
        assertEquals("apple", ((JsonString) arr.value().get(0)).value());
        assertEquals("banana", ((JsonString) arr.value().get(1)).value());
        assertEquals("cherry", ((JsonString) arr.value().get(2)).value());
    }

    @Test
    void testNestedObjects() {
        JsonObject obj = JsonReader.of("{\"user\":{\"name\":\"Alice\",\"email\":\"alice@example.com\"},\"active\":true}")
                .read();
        assertNotNull(obj);
        JsonObject user = (JsonObject) obj.get("user");
        assertEquals("Alice", ((JsonString) user.get("name")).value());
        assertEquals("alice@example.com", ((JsonString) user.get("email")).value());
        assertTrue(((JsonBoolean) obj.get("active")).value());
    }

    @Test
    void testNestedArrays() {
        JsonArray arr = JsonReader.of("[[1,2],[3,4]]").read();
        assertNotNull(arr);
        assertEquals(2, arr.size());
        JsonArray first = (JsonArray) arr.value().get(0);
        JsonArray second = (JsonArray) arr.value().get(1);
        assertEquals(1L, ((JsonInteger) first.value().get(0)).longValue());
        assertEquals(2L, ((JsonInteger) first.value().get(1)).longValue());
        assertEquals(3L, ((JsonInteger) second.value().get(0)).longValue());
        assertEquals(4L, ((JsonInteger) second.value().get(1)).longValue());
    }

    @Test
    void testMixedTypes() {
        JsonObject obj = JsonReader
                .of("{\"string\":\"value\",\"integer\":42,\"long\":9876543210,\"boolean\":false}").read();
        assertNotNull(obj);
        assertEquals("value", ((JsonString) obj.get("string")).value());
        assertEquals(42L, ((JsonInteger) obj.get("integer")).longValue());
        assertEquals(9876543210L, ((JsonInteger) obj.get("long")).longValue());
        assertFalse(((JsonBoolean) obj.get("boolean")).value());
    }

    @Test
    void testEmptyObject() {
        JsonObject obj = JsonReader.of("{}").read();
        assertNotNull(obj);
        assertEquals(0, obj.members().size());
    }

    @Test
    void testEmptyArray() {
        JsonArray arr = JsonReader.of("[]").read();
        assertNotNull(arr);
        assertEquals(0, arr.size());
    }

    @Test
    void testStringEscapingQuotes() {
        JsonObject obj = JsonReader.of("{\"message\":\"He said \\\"Hello\\\"\"}").read();
        assertEquals("He said \"Hello\"", ((JsonString) obj.get("message")).value());
    }

    @Test
    void testStringEscapingBackslash() {
        JsonObject obj = JsonReader.of("{\"path\":\"C:\\\\Users\\\\John\"}").read();
        assertEquals("C:\\Users\\John", ((JsonString) obj.get("path")).value());
    }

    @Test
    void testStringEscapingControlCharacters() {
        JsonObject obj = JsonReader.of("{\"text\":\"Line1\\nLine2\\tTabbed\\rReturn\"}").read();
        assertEquals("Line1\nLine2\tTabbed\rReturn", ((JsonString) obj.get("text")).value());
    }

    @Test
    void testStringEscapingUnicodeSequences() {
        JsonObject obj = JsonReader.of("{\"text\":\"\\u0048\\u0065\\u006c\\u006c\\u006f\"}").read();
        assertEquals("Hello", ((JsonString) obj.get("text")).value());
    }

    @Test
    void testStringEscapingAllControlCharacters() {
        String json = "{\"controls\":\"\\u0000\\u0001\\u0002\\u0003\\u0004\\u0005\\u0006\\u0007" +
                "\\u0008\\u0009\\u000a\\u000b\\u000c\\u000d\\u000e\\u000f" +
                "\\u0010\\u0011\\u0012\\u0013\\u0014\\u0015\\u0016\\u0017" +
                "\\u0018\\u0019\\u001a\\u001b\\u001c\\u001d\\u001e\\u001f\"}";
        JsonObject obj = JsonReader.of(json).read();
        String value = ((JsonString) obj.get("controls")).value();
        assertEquals(32, value.length());
        for (int i = 0; i <= 0x1f; i++) {
            assertEquals((char) i, value.charAt(i));
        }
    }

    @Test
    void testStringWithSpecialCharacters() {
        JsonObject obj = JsonReader.of("{\"special\":\"äöü ñ €\"}").read();
        assertEquals("äöü ñ €", ((JsonString) obj.get("special")).value());
    }

    @Test
    void testEmptyString() {
        JsonObject obj = JsonReader.of("{\"empty\":\"\"}").read();
        assertEquals("", ((JsonString) obj.get("empty")).value());
    }

    @Test
    void testArrayWithMixedTypes() {
        JsonArray arr = JsonReader.of("[\"text\",123,true,{\"key\":\"value\"}]").read();
        assertEquals(4, arr.size());
        assertEquals("text", ((JsonString) arr.value().get(0)).value());
        assertEquals(123L, ((JsonInteger) arr.value().get(1)).longValue());
        assertTrue(((JsonBoolean) arr.value().get(2)).value());
        JsonObject obj = (JsonObject) arr.value().get(3);
        assertEquals("value", ((JsonString) obj.get("key")).value());
    }

    @Test
    void testComplexNestedStructure() {
        JsonObject obj = JsonReader
                .of("{\"users\":[{\"id\":1,\"name\":\"Alice\"},{\"id\":2,\"name\":\"Bob\"}],\"count\":2}").read();
        JsonArray users = (JsonArray) obj.get("users");
        assertEquals(2, users.size());
        JsonObject alice = (JsonObject) users.value().get(0);
        assertEquals(1L, ((JsonInteger) alice.get("id")).longValue());
        assertEquals("Alice", ((JsonString) alice.get("name")).value());
        JsonObject bob = (JsonObject) users.value().get(1);
        assertEquals(2L, ((JsonInteger) bob.get("id")).longValue());
        assertEquals("Bob", ((JsonString) bob.get("name")).value());
        assertEquals(2L, ((JsonInteger) obj.get("count")).longValue());
    }

    @Test
    void testWhitespaceHandling() {
        JsonObject obj = JsonReader.of("  {  \"name\"  :  \"John\"  ,  \"age\"  :  30  }  ").read();
        assertEquals("John", ((JsonString) obj.get("name")).value());
        assertEquals(30L, ((JsonInteger) obj.get("age")).longValue());
    }

    @Test
    void testNullValue() {
        JsonObject obj = JsonReader.of("{\"value\":null}").read();
        assertInstanceOf(JsonNull.class, obj.get("value"));
    }

    @Test
    void testBooleanValues() {
        JsonObject obj = JsonReader.of("{\"trueVal\":true,\"falseVal\":false}").read();
        assertTrue(((JsonBoolean) obj.get("trueVal")).value());
        assertFalse(((JsonBoolean) obj.get("falseVal")).value());
    }

    @Test
    void testNumberFormats() {
        JsonObject obj = JsonReader.of("{\"int\":42,\"negative\":-17,\"zero\":0,\"decimal\":3.14,\"exp\":1.0e10}")
                .read();
        assertEquals(42L, ((JsonInteger) obj.get("int")).longValue());
        assertEquals(-17L, ((JsonInteger) obj.get("negative")).longValue());
        assertEquals(0L, ((JsonInteger) obj.get("zero")).longValue());
        assertEquals(3.14, ((JsonDouble) obj.get("decimal")).value(), 0.001);
        assertEquals(1.0e10, ((JsonDouble) obj.get("exp")).value(), 0.001);
    }

    @Test
    void testLargeNumbers() {
        JsonObject obj = JsonReader
                .of("{\"maxInt\":2147483647,\"minInt\":-2147483648,\"maxLong\":9223372036854775807,\"minLong\":-9223372036854775808}")
                .read();
        assertEquals(Integer.MAX_VALUE, ((JsonInteger) obj.get("maxInt")).longValue());
        assertEquals(Integer.MIN_VALUE, ((JsonInteger) obj.get("minInt")).longValue());
        assertEquals(Long.MAX_VALUE, ((JsonInteger) obj.get("maxLong")).longValue());
        assertEquals(Long.MIN_VALUE, ((JsonInteger) obj.get("minLong")).longValue());
    }

    @Test
    void testStringWithQuotesAndBackslashes() {
        JsonObject obj = JsonReader.of("{\"complex\":\"\\\"\\\\\\\"\\\\\\\\\\\"\"}").read();
        assertEquals("\"\\\"\\\\\"", ((JsonString) obj.get("complex")).value());
    }

    @Test
    void testInvalidJsonMissingClosingBrace() {
        assertThrows(IllegalArgumentException.class, () -> JsonReader.of("{\"name\":\"John\"").read());
    }

    @Test
    void testInvalidJsonMissingClosingBracket() {
        assertThrows(IllegalArgumentException.class, () -> JsonReader.of("[1,2,3").read());
    }

    @Test
    void testInvalidJsonMissingColon() {
        assertThrows(IllegalArgumentException.class, () -> JsonReader.of("{\"name\" \"John\"}").read());
    }

    @Test
    void testInvalidJsonUnquotedString() {
        assertThrows(IllegalArgumentException.class, () -> JsonReader.of("{\"name\":John}").read());
    }

    @Test
    void testInvalidJsonControlCharacterInString() {
        assertThrows(IllegalArgumentException.class, () -> JsonReader.of("{\"text\":\"Line1\nLine2\"}").read());
    }

    @Test
    void testStringWithForwardSlashEscape() {
        JsonObject obj = JsonReader.of("{\"url\":\"https:\\/\\/example.com\"}").read();
        assertEquals("https://example.com", ((JsonString) obj.get("url")).value());
    }

    @Test
    void testStringWithBackspaceAndFormFeed() {
        JsonObject obj = JsonReader.of("{\"text\":\"Hello\\b\\f\"}").read();
        assertEquals("Hello\b\f", ((JsonString) obj.get("text")).value());
    }
}
