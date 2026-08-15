package org.acme.example.extension.deployment;

import org.acme.liba.LibA;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;





class QuarkusExampleProcessor {

    private static final String BUILD_MESSAGE_PROPERTY = "org.acme.example.extension.build-message";
    private static final String FEATURE = "example";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    SystemPropertyBuildItem buildMessage(QuarkusExampleBuildConfig config) {
        return new SystemPropertyBuildItem(BUILD_MESSAGE_PROPERTY, config.buildMessage());
    }

    @BuildStep
    void addLibABean(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(new AdditionalBeanBuildItem.Builder()
                .addBeanClasses(LibA.class)
                .setUnremovable()
                .setDefaultScope(DotNames.APPLICATION_SCOPED)
                .build());
    }

}
