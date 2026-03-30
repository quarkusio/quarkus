package io.quarkus.vertx.runtime.jackson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class JsonUtilTest {

    @Test
    public void wrapJsonValueNull() {
        assertNull(JsonUtil.wrapJsonValue(null));
    }

    @Test
    public void wrapJsonValueMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");
        Object result = JsonUtil.wrapJsonValue(map);
        assertThat(result).isInstanceOf(JsonObject.class);
        assertThat(((JsonObject) result).getString("key")).isEqualTo("value");
    }

    @Test
    public void wrapJsonValueList() {
        List<Object> list = Arrays.asList("a", "b", "c");
        Object result = JsonUtil.wrapJsonValue(list);
        assertThat(result).isInstanceOf(JsonArray.class);
        assertThat(((JsonArray) result).size()).isEqualTo(3);
        assertThat(((JsonArray) result).getString(0)).isEqualTo("a");
    }

    @Test
    public void wrapJsonValueInstant() {
        Instant now = Instant.parse("2024-01-15T10:30:00Z");
        Object result = JsonUtil.wrapJsonValue(now);
        assertThat(result).isInstanceOf(String.class);
        assertThat((String) result).isEqualTo("2024-01-15T10:30:00Z");
    }

    @Test
    public void wrapJsonValueByteArray() {
        byte[] bytes = new byte[] { 1, 2, 3, 4 };
        Object result = JsonUtil.wrapJsonValue(bytes);
        assertThat(result).isInstanceOf(String.class);
        byte[] decoded = JsonUtil.BASE64_DECODER.decode((String) result);
        assertThat(decoded).isEqualTo(bytes);
    }

    @Test
    public void wrapJsonValueBuffer() {
        Buffer buffer = Buffer.buffer(new byte[] { 5, 6, 7 });
        Object result = JsonUtil.wrapJsonValue(buffer);
        assertThat(result).isInstanceOf(String.class);
        byte[] decoded = JsonUtil.BASE64_DECODER.decode((String) result);
        assertThat(decoded).isEqualTo(new byte[] { 5, 6, 7 });
    }

    @Test
    public void wrapJsonValueEnum() {
        Object result = JsonUtil.wrapJsonValue(Thread.State.RUNNABLE);
        assertThat(result).isInstanceOf(String.class);
        assertThat(result).isEqualTo("RUNNABLE");
    }

    @Test
    public void wrapJsonValuePassthroughString() {
        String value = "hello";
        Object result = JsonUtil.wrapJsonValue(value);
        assertSame(value, result);
    }

    @Test
    public void wrapJsonValuePassthroughInteger() {
        Integer value = 42;
        Object result = JsonUtil.wrapJsonValue(value);
        assertSame(value, result);
    }

    @Test
    public void wrapJsonValuePassthroughBoolean() {
        Object result = JsonUtil.wrapJsonValue(Boolean.TRUE);
        assertSame(Boolean.TRUE, result);
    }

    @Test
    public void checkAndCopyNull() {
        assertNull(JsonUtil.checkAndCopy(null));
    }

    @Test
    public void checkAndCopyNumber() {
        Integer val = 42;
        assertSame(val, JsonUtil.checkAndCopy(val));
    }

    @Test
    public void checkAndCopyLong() {
        Long val = 42L;
        assertSame(val, JsonUtil.checkAndCopy(val));
    }

    @Test
    public void checkAndCopyBoolean() {
        assertSame(Boolean.TRUE, JsonUtil.checkAndCopy(Boolean.TRUE));
    }

    @Test
    public void checkAndCopyString() {
        String val = "hello";
        assertSame(val, JsonUtil.checkAndCopy(val));
    }

    @Test
    public void checkAndCopyCharacter() {
        Character val = 'x';
        assertSame(val, JsonUtil.checkAndCopy(val));
    }

    @Test
    public void checkAndCopyCharSequence() {
        StringBuilder sb = new StringBuilder("mutable");
        Object result = JsonUtil.checkAndCopy(sb);
        assertThat(result).isInstanceOf(String.class);
        assertThat(result).isEqualTo("mutable");
    }

    @Test
    public void checkAndCopyJsonObject() {
        JsonObject original = new JsonObject().put("key", "value");
        Object result = JsonUtil.checkAndCopy(original);
        assertThat(result).isInstanceOf(JsonObject.class);
        assertThat(result).isNotSameAs(original); // copied
        assertThat(((JsonObject) result).getString("key")).isEqualTo("value");
    }

    @Test
    public void checkAndCopyJsonArray() {
        JsonArray original = new JsonArray().add(1).add(2);
        Object result = JsonUtil.checkAndCopy(original);
        assertThat(result).isInstanceOf(JsonArray.class);
        assertThat(result).isNotSameAs(original); // copied
        assertThat(((JsonArray) result).size()).isEqualTo(2);
    }

    @Test
    public void checkAndCopyMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");
        Object result = JsonUtil.checkAndCopy(map);
        assertThat(result).isInstanceOf(JsonObject.class);
        assertThat(((JsonObject) result).getString("key")).isEqualTo("value");
    }

    @Test
    public void checkAndCopyList() {
        List<Object> list = Arrays.asList("a", "b");
        Object result = JsonUtil.checkAndCopy(list);
        assertThat(result).isInstanceOf(JsonArray.class);
        assertThat(((JsonArray) result).size()).isEqualTo(2);
    }

    @Test
    public void checkAndCopyBuffer() {
        Buffer original = Buffer.buffer("hello");
        Object result = JsonUtil.checkAndCopy(original);
        assertThat(result).isInstanceOf(Buffer.class);
        assertThat(result).isNotSameAs(original); // copied
        assertThat(((Buffer) result).toString()).isEqualTo("hello");
    }

    @Test
    public void checkAndCopyByteArray() {
        byte[] val = new byte[] { 1, 2, 3 };
        assertSame(val, JsonUtil.checkAndCopy(val));
    }

    @Test
    public void checkAndCopyInstant() {
        Instant val = Instant.now();
        assertSame(val, JsonUtil.checkAndCopy(val));
    }

    @Test
    public void checkAndCopyEnum() {
        Thread.State val = Thread.State.RUNNABLE;
        assertSame(val, JsonUtil.checkAndCopy(val));
    }

    @Test
    public void checkAndCopyUnsupportedType() {
        // Use a type that is not in the supported set (not Number, Boolean, String, CharSequence,
        // Shareable, Map, List, Buffer, byte[], Instant, Enum, or Character)
        assertThatThrownBy(() -> JsonUtil.checkAndCopy(new java.util.Date()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Illegal type in Json");
    }
}
