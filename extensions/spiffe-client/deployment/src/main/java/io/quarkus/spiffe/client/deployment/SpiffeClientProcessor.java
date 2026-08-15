package io.quarkus.spiffe.client.deployment;

import static io.quarkus.arc.processor.DotNames.APPLICATION_SCOPED;

import java.util.function.BooleanSupplier;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

final class SpiffeClientProcessor {

    private static final String CLIENT_IMPL_CLASS = "io.quarkus.spiffe.client.runtime.internal.SpiffeClientImpl";
    static final String FEATURE = "spiffe-client";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = SpiffeClientEnabled.class)
    AdditionalBeanBuildItem registerClientAsCdiBean() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(CLIENT_IMPL_CLASS)
                .setDefaultScope(APPLICATION_SCOPED)
                .setUnremovable()
                .build();
    }

    static final class SpiffeClientEnabled implements BooleanSupplier {

        private final boolean enabled;

        SpiffeClientEnabled(SpiffeClientBuildTimeConfig config) {
            this.enabled = config.enabled();
        }

        @Override
        public boolean getAsBoolean() {
            return enabled;
        }
    }
}
