package io.quarkus.oidc.token.propagation.graphql.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class OidcTokenPropagationGraphQLProcessor {

    private static final String FEATURE = "smallrye-graphql-client-oidc-token-propagation";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }
}
