package io.quarkus.vertx.runtime.jackson;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.Shareable;

/**
 * Implementation utilities (details) affecting the way JSON objects are wrapped.
 *
 * This class is copied from {@code io.vertx.core.json.impl.JsonUtil} as it is internal to Vert.x
 */
final class JsonUtil {

    public static final Base64.Encoder BASE64_ENCODER;
    public static final Base64.Decoder BASE64_DECODER;

    static {
        /*
         * Vert.x 3.x Json supports RFC-7493, however the JSON encoder/decoder format was incorrect.
         * Users who might need to interop with Vert.x 3.x applications should set the system property
         * {@code vertx.json.base64} to {@code legacy}.
         */
        if ("legacy".equalsIgnoreCase(System.getProperty("vertx.json.base64"))) { //TODO: do we need this for Quarkus?
            BASE64_ENCODER = Base64.getEncoder();
            BASE64_DECODER = Base64.getDecoder();
        } else {
            BASE64_ENCODER = Base64.getUrlEncoder().withoutPadding();
            BASE64_DECODER = Base64.getUrlDecoder();
        }
    }

    /**
     * Wraps well known java types to adhere to the Json expected types.
     * <ul>
     * <li>{@code Map} will be wrapped to {@code JsonObject}</li>
     * <li>{@code List} will be wrapped to {@code JsonArray}</li>
     * <li>{@code Instant} will be converted to iso date {@code String}</li>
     * <li>{@code byte[]} will be converted to base64 {@code String}</li>
     * <li>{@code Enum} will be converted to enum name {@code String}</li>
     * </ul>
     *
     * @param val java type
     * @return wrapped type or {@code val} if not applicable.
     */
    public static Object wrapJsonValue(Object val) {
        if (val == null) {
            return null;
        }

        // perform wrapping
        if (val instanceof Map) {
            val = new JsonObject((Map) val);
        } else if (val instanceof List) {
            val = new JsonArray((List) val);
        } else if (val instanceof Instant) {
            val = ISO_INSTANT.format((Instant) val);
        } else if (val instanceof byte[]) {
            val = BASE64_ENCODER.encodeToString((byte[]) val);
        } else if (val instanceof Buffer) {
            val = BASE64_ENCODER.encodeToString(((Buffer) val).getBytes());
        } else if (val instanceof Enum) {
            val = ((Enum) val).name();
        }

        return val;
    }

    @SuppressWarnings("unchecked")
    public static Object checkAndCopy(Object val) {
        if (val == null) {
            // OK
        } else if (val instanceof Number) {
            // OK
        } else if (val instanceof Boolean) {
            // OK
        } else if (val instanceof String) {
            // OK
        } else if (val instanceof Character) {
            // OK
        } else if (val instanceof CharSequence) {
            // CharSequences are not immutable, so we force toString() to become immutable
            val = val.toString();
        } else if (val instanceof Shareable) {
            // Shareable objects know how to copy themselves, this covers:
            // JsonObject, JsonArray or any user defined type that can shared across the cluster
            val = ((Shareable) val).copy();
        } else if (val instanceof Map) {
            val = (new JsonObject((Map) val)).copy();
        } else if (val instanceof List) {
            val = (new JsonArray((List) val)).copy();
        } else if (val instanceof Buffer) {
            val = ((Buffer) val).copy();
        } else if (val instanceof byte[]) {
            // OK
        } else if (val instanceof Instant) {
            // OK
        } else if (val instanceof Enum) {
            // OK
        } else {
            throw new IllegalStateException("Illegal type in Json: " + val.getClass());
        }
        return val;
    }
}
