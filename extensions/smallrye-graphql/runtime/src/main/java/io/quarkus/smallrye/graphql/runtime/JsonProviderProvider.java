package io.quarkus.smallrye.graphql.runtime;

import jakarta.json.spi.JsonProvider;

public class JsonProviderProvider {
    public static final JsonProvider JSON_PROVIDER = JsonProvider.provider();
}
