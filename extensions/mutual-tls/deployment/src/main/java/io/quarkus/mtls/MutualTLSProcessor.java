package io.quarkus.mtls;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;

public class MutualTLSProcessor {

    @BuildStep
    AdditionalBeanBuildItem registerAdditionalBeans() {
        return new AdditionalBeanBuildItem.Builder()
                .setUnremovable()
                .addBeanClass(MutualTLSProvider.class)
                .build();
    }

}
