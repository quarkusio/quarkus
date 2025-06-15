package io.quarkus.redis.datasource.search;

import java.util.Collections;
import java.util.Map;

import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.redis.client.Response;

/**
 * Represents a document containing in the response of a {@code ft.search} command.
 */
public class Document {

    private final String id;
    private final double score;
    private final Map<String, Property> properties;
    private final Response payload;

    public Document(String id, double score, Response payload, Map<String, Property> properties) {
        this.id = id;
        this.score = score == 0 ? 1.0 : score;
        this.payload = payload;
        this.properties = properties == null ? Collections.emptyMap() : Collections.unmodifiableMap(properties);
    }

    /**
     * @return the document key
     */
    public String key() {
        return id;
    }

    /**
     * @return the score, 0.0 if not requested
     */
    public double score() {
        return score;
    }

    /**
     * @return the list of properties of the document
     */
    public Map<String, Property> properties() {
        return properties;
    }

    /**
     * Gets a specific property from the document
     *
     * @param name
     *        the name, must not be {@code null}
     *
     * @return the property, {@code null} if not found
     */
    public Property property(String name) {
        return properties.get(name);
    }

    /**
     * @return the payload
     */
    public Response payload() {
        return payload;
    }

    /**
     * Represents a document property / attribute
     */
    public static class Property {

        private final Response response;
        private final String name;

        public Property(String name, Response response) {
            this.response = response;
            this.name = name;
        }

        /**
         * @return the property value as double
         */
        public double asDouble() {
            return response.toDouble();
        }

        /**
         * @return the property value as integer
         */
        public int asInteger() {
            return response.toInteger();
        }

        /**
         * @return the property value as long
         */
        public long asLong() {
            return response.toLong();
        }

        /**
         * @return the raw property value
         */
        public Response unwrap() {
            return response;
        }

        /**
         * @return the property value as string
         */
        public String asString() {
            return response.toString();
        }

        /**
         * @return the property value as byte array
         */
        public byte[] asBytes() {
            return response.toBytes();
        }

        /**
         * @return the property value as boolean
         */
        public boolean asBoolean() {
            return response.toBoolean();
        }

        /**
         * @return the property name
         */
        public String name() {
            return name;
        }

        /**
         * @return the property value as JSON Object
         */
        public JsonObject asJsonObject() {
            return response.toBuffer().toJsonObject();
        }
    }

}
