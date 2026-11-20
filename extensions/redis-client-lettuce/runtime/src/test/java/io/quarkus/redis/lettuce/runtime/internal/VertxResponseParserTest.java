package io.quarkus.redis.lettuce.runtime.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.output.ComplexOutput;
import io.vertx.redis.client.Response;
import io.vertx.redis.client.ResponseType;
import io.vertx.redis.client.impl.types.MultiType;

class VertxResponseParserTest {

    @Test
    void arraysBecomePlainMultis() {
        Response reply = parse(List.of(2L, "doc:1", List.of("title", "Heat", "rating", 8.2d)));

        assertThat(reply.type()).isEqualTo(ResponseType.MULTI);
        assertThat(((MultiType) reply).isMap()).isFalse();
        assertThat(reply.size()).isEqualTo(3);
        assertThat(reply.get(0).toInteger()).isEqualTo(2);
        assertThat(reply.get(1).toString()).isEqualTo("doc:1");
        assertThat(reply.get(2).get(1).toString()).isEqualTo("Heat");
        assertThat(reply.get(2).get(3).toDouble()).isEqualTo(8.2);
    }

    @Test
    void mapsBecomeMapMultis() {
        Map<Object, Object> result = new LinkedHashMap<>();
        result.put("id", "doc:1");
        result.put("extra_attributes", Map.of("title", "Heat"));
        Map<Object, Object> reply = new LinkedHashMap<>();
        reply.put("total_results", 1L);
        reply.put("results", List.of(result));

        Response response = parse(reply);

        assertThat(((MultiType) response).isMap()).isTrue();
        assertThat(response.getKeys()).containsExactlyInAnyOrder("total_results", "results");
        assertThat(response.get("total_results").toLong()).isEqualTo(1L);
        Response first = response.get("results").get(0);
        assertThat(first.get("id").toString()).isEqualTo("doc:1");
        assertThat(first.get("extra_attributes").get("title").toString()).isEqualTo("Heat");
    }

    @Test
    void nullsAndBooleansKeepTheirMeaning() {
        Response reply = parse(Arrays.asList(null, true, false));

        assertThat(reply.get(0)).isNull();
        assertThat(reply.get(1).toBoolean()).isTrue();
        assertThat(reply.get(2).toBoolean()).isFalse();
    }

    @Test
    void nullReplyIsNull() {
        ComplexOutput<byte[], byte[], Response> output = new ComplexOutput<>(ByteArrayCodec.INSTANCE,
                VertxResponseParser.INSTANCE);
        output.set((ByteBuffer) null);

        assertThat(output.get()).isNull();
    }

    private static Response parse(Object reply) {
        ComplexOutput<byte[], byte[], Response> output = new ComplexOutput<>(ByteArrayCodec.INSTANCE,
                VertxResponseParser.INSTANCE);
        write(output, reply, 0);
        return output.get();
    }

    /**
     * Feeds {@code value} to the output the way Lettuce's protocol state machine does: an aggregate is opened, its
     * elements written, then completed with its nesting depth.
     */
    private static void write(ComplexOutput<?, ?, ?> output, Object value, int depth) {
        if (value instanceof Map<?, ?> map) {
            output.multiMap(map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                write(output, entry.getKey(), depth + 1);
                write(output, entry.getValue(), depth + 1);
            }
        } else if (value instanceof List<?> list) {
            output.multiArray(list.size());
            for (Object item : list) {
                write(output, item, depth + 1);
            }
        } else if (value instanceof String string) {
            output.set(ByteBuffer.wrap(string.getBytes(StandardCharsets.UTF_8)));
        } else if (value instanceof Long number) {
            output.set(number.longValue());
        } else if (value instanceof Double number) {
            output.set(number.doubleValue());
        } else if (value instanceof Boolean bool) {
            output.set(bool.booleanValue());
        } else if (value == null) {
            output.set((ByteBuffer) null);
        } else {
            throw new IllegalArgumentException("Unsupported test value: " + value);
        }
        output.complete(depth);
    }

}
