package io.quarkus.oidc.cache.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.oidc.runtime.CacheTokenStateManager;

class OidcCodeTokensCacheProcessor {

    private static final String FEATURE = "oidc-code-tokens-cache";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem cacheTokenStateManager(Capabilities capabilities) {

        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder().setUnremovable();

        builder.addBeanClass(CacheTokenStateManager.class);

        return builder.build();
    }

}
