package io.quarkus.oidc.persistence.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.oidc.runtime.PersistenceTokenStateManager;

class OidcPersistenceProcessor {

    private static final String FEATURE = "oidc-code-tokens-persistence";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem persistenceTokenStateManager(Capabilities capabilities) {

        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder().setUnremovable();

        builder.addBeanClass(PersistenceTokenStateManager.class);

        return builder.build();
    }

}
