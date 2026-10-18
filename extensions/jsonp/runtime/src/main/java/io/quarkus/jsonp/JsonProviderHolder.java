package io.quarkus.jsonp;

import jakarta.json.spi.JsonProvider;

/**
 * A utility class that holds an instance of {@link JsonProvider}.
 * Should be used instead of {@link jakarta.json.Json} for performance reasons.
 *
 * @see <a href="https://github.com/jakartaee/jsonp-api/issues/154">Json factory methods are very inefficient</a>
 */
public final class JsonProviderHolder {

    private static final JsonProvider JSON_PROVIDER = JsonProvider.provider();

    private JsonProviderHolder() {
    }

    public static JsonProvider jsonProvider() {
        return JSON_PROVIDER;
    }
}
