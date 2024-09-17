package io.quarkus.grpc.runtime.transcoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.grpc.transcoding.GrpcTranscodingMessageWriter;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.DecodeException;

/**
 * Tests for {@link GrpcTranscodingMessageWriter}. In this test class, we test the utility methods that are used to merge
 * path parameters, query parameters, and payload into a single map. This should cover most of the cases which can be
 * encountered in the real world.
 */
class GrpcTranscodingMessageWriterTest {

    @Test
    @DisplayName("Test mergeParameters with all parameters")
    void testMergeParametersAllParameters() {
        Map<String, String> pathParams = Map.of("id", "123");
        Map<String, String> queryParams = Map.of("name", "test");
        Buffer payload = Buffer.buffer("{\"field\": \"value\"}");

        Map<String, Object> expected = Map.of(
                "id", "123",
                "name", "test",
                "field", "value");

        assertEquals(expected, GrpcTranscodingMessageWriter.mergeParameters(pathParams, queryParams, payload));
    }

    @Test
    @DisplayName("Test mergeParameters with no payload")
    void testMergeParametersNoPayload() {
        Map<String, String> pathParams = Map.of("id", "123");
        Map<String, String> queryParams = Map.of("name", "test");

        Map<String, Object> expected = Map.of(
                "id", "123",
                "name", "test");

        assertEquals(expected, GrpcTranscodingMessageWriter.mergeParameters(pathParams, queryParams, null));
    }

    @Test
    @DisplayName("Test mergeParameters with invalid payload")
    void testMergeParametersInvalidPayload() {
        Buffer invalidPayload = Buffer.buffer("this is not json");
        assertThrows(DecodeException.class, () -> {
            GrpcTranscodingMessageWriter.mergeParameters(new HashMap<>(), new HashMap<>(), invalidPayload);
        });
    }

    @Test
    @DisplayName("Test mergeParameters with nested parameters")
    void testMergeParametersNested() {
        Map<String, String> pathParams = Map.of("order.id", "123");
        Map<String, String> queryParams = Map.of("customer.name", "test");

        Map<String, Object> expected = new HashMap<>();
        expected.put("order", Map.of("id", "123"));
        expected.put("customer", Map.of("name", "test"));

        assertEquals(expected, GrpcTranscodingMessageWriter.mergeParameters(pathParams, queryParams, null));
    }

    @Test
    @DisplayName("Test insertNestedParam with simple key")
    void testInsertNestedParamSimple() {
        Map<String, Object> params = new HashMap<>();
        GrpcTranscodingMessageWriter.insertNestedParam(params, "level1.level2.value", "test");

        Map<String, Object> level1 = (Map<String, Object>) params.get("level1");
        assertNotNull(level1);

        Map<String, Object> level2 = (Map<String, Object>) level1.get("level2");
        assertNotNull(level2);

        assertEquals("test", level2.get("value"));
    }

    @Test
    @DisplayName("Test insertNestedParam with existing structure")
    void testInsertNestedParamExistingStructure() {
        Map<String, Object> params = new HashMap<>();
        params.put("level1", new HashMap<>(Map.of("existing", "value")));

        GrpcTranscodingMessageWriter.insertNestedParam(params, "level1.level2.value", "test");

        Map<String, Object> level1 = (Map<String, Object>) params.get("level1");
        assertNotNull(level1);

        assertEquals("value", level1.get("existing"));

        Map<String, Object> level2 = (Map<String, Object>) level1.get("level2");
        assertNotNull(level2);

        assertEquals("test", level2.get("value"));
    }
}
