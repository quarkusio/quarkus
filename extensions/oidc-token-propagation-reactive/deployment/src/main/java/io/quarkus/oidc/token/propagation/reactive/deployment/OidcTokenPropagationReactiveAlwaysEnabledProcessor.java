package io.quarkus.oidc.token.propagation.reactive.deployment;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

// Executed even if the extension is disabled, see https://github.com/quarkusio/quarkus/pull/26966/
public class OidcTokenPropagationReactiveAlwaysEnabledProcessor {

    @BuildStep
    FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem(Feature.REST_CLIENT_OIDC_TOKEN_PROPAGATION);
    }

}
