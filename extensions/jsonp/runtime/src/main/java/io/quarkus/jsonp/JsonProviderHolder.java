package io.quarkus.jsonp;

import jakarta.json.spi.JsonProvider;

public final class JsonProviderHolder {

    private JsonProviderHolder() {
    }

    public static final JsonProvider JSON_PROVIDER = JsonProvider.provider();
}
