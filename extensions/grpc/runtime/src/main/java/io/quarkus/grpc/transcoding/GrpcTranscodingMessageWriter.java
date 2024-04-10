package io.quarkus.grpc.transcoding;

import java.util.HashMap;
import java.util.Map;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;

/**
 * The `GrpcTranscodingMessageWriter` class assists with the manipulation of gRPC
 * message payloads during the transcoding process. Its responsibilities include:
 * <p>
 * Merging existing JSON payloads, path parameters, and query parameters into a
 * unified map representation.
 * Providing the logic for inserting nested parameters within the generated map.
 */
public class GrpcTranscodingMessageWriter {

    private final static String SEPARATOR = "\\.";

    /**
     * Merges path parameters, query parameters, and an optional existing JSON payload
     * into a single `Map` object. This method provides a centralized way to combine
     * parameters during gRPC message transcoding.
     *
     * @param pathParams A map containing path parameters extracted from the request.
     * @param queryParams A map containing query parameters extracted from the request.
     * @param existingPayload An optional Vert.x `Buffer` containing an existing JSON payload.
     * @return A `Map<String, Object>` representing the merged parameters.
     * @throws IllegalArgumentException If the provided `existingPayload` cannot be parsed as valid JSON.
     */
    public static Map<String, Object> mergeParameters(Map<String, String> pathParams, Map<String, String> queryParams,
            Buffer existingPayload) {
        Map<String, Object> allParams = new HashMap<>();

        if (existingPayload != null && existingPayload.getBytes().length > 0) {
            try {
                String existingPayloadJson = new String(existingPayload.getBytes());
                allParams = new HashMap<String, Object>(Json.decodeValue(existingPayloadJson, Map.class));
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid payload", e);
            }
        }

        for (Map.Entry<String, String> entry : pathParams.entrySet()) {
            insertNestedParam(allParams, entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            insertNestedParam(allParams, entry.getKey(), entry.getValue());
        }

        return allParams;
    }

    /**
     * Inserts a key-value pair into a nested structure within a `Map`. This method supports
     * the creation of hierarchical parameter structures during the transcoding process.
     * Key components are separated by periods ('.').
     *
     * @param paramsMap The `Map` object where the nested parameter will be inserted.
     * @param key The parameter key, potentially containing periods for nested structures.
     * @param value The parameter value to be inserted.
     */
    public static void insertNestedParam(Map<String, Object> paramsMap, String key, String value) {
        String[] pathComponents = key.split(SEPARATOR);

        Map<String, Object> currentLevel = paramsMap;
        for (int i = 0; i < pathComponents.length - 1; i++) {
            String component = pathComponents[i];
            if (!currentLevel.containsKey(component)) {
                currentLevel.put(component, new HashMap<>());
            }
            currentLevel = (Map<String, Object>) currentLevel.get(component);
        }

        currentLevel.put(pathComponents[pathComponents.length - 1], value);
    }
}
