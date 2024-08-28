package io.quarkus.jsonp.runtime;

import jakarta.json.spi.JsonProvider;

public final class JsonProviderHolder {

    private JsonProviderHolder() {
    }

    public static final JsonProvider JSON_PROVIDER = JsonProvider.provider();
}
