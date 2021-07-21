package io.quarkus.spring.cloud.config.client.oidc;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.spring.cloud.config.client.runtime.SpringCloudConfigClientOidcProvider;

public class SpringCloudConfigClientOidcProcessor {

    @BuildStep
    public void feature(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(Feature.SPRING_CLOUD_CONFIG_CLIENT_OIDC));
    }

    @BuildStep
    public void configure(
            BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuild) {
        additionalBeanBuildItemBuild.produce(AdditionalBeanBuildItem.unremovableOf(SpringCloudConfigClientOidcProvider.class));
    }
}
