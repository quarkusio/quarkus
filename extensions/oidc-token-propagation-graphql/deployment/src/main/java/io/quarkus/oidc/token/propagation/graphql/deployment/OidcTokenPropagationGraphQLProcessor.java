package io.quarkus.oidc.token.propagation.graphql.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.oidc.token.propagation.graphql.runtime.OidcTokenPropagationGraphQLBuildTimeConfig;

class OidcTokenPropagationGraphQLProcessor {

    @BuildStep(onlyIf = IsEnabled.class)
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.SMALLRYE_GRAPHQL_CLIENT_OIDC_TOKEN_PROPAGATION);
    }

    static class IsEnabled implements BooleanSupplier {
        OidcTokenPropagationGraphQLBuildTimeConfig config;

        public boolean getAsBoolean() {
            return config.enabled();
        }
    }
}
