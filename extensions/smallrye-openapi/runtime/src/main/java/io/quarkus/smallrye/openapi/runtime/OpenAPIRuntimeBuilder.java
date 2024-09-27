package io.quarkus.smallrye.openapi.runtime;

import io.smallrye.openapi.api.SmallRyeOpenAPI;

/**
 * Customized {@link SmallRyeOpenAPI.Builder} implementation that only includes
 * functionality that should occur at application runtime. Specifically, it only
 * supports loading a static OpenAPI file/stream, applying OASFilter instances,
 * and writing the OpenAPI model to a DOM for later serialization to JSON or
 * YAML.
 */
class OpenAPIRuntimeBuilder extends SmallRyeOpenAPI.Builder {
    @Override
    public <V, A extends V, O extends V, AB, OB> SmallRyeOpenAPI build() {
        var ctx = super.getContext();
        buildPrepare(ctx);
        buildStaticModel(ctx);
        return buildFinalize(ctx);
    }
}
