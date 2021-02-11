package io.quarkus.consul.config.runtime;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Represent (part of) the JSON response from Consul
 *
 * That means that the the key and value are exactly what we get back from Consul
 */
public class Response {

    private final String key;
    private final String value;

    public Response(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    /**
     * Consul encodes the value of a key into base64 in order to not interfere with the encoding of format of the response
     */
    public String getDecodedValue() {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
