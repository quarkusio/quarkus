package io.quarkus.elasticsearch.restclient.lowlevel.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.elasticsearch.restclient.lowlevel.ElasticsearchClientConfig;
import io.quarkus.elasticsearch.restclient.lowlevel.runtime.ElasticsearchRestClientProducer;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

class ElasticsearchLowLevelClientProcessor {

    private static final DotName ELASTICSEARCH_CLIENT_CONFIG = DotName.createSimple(ElasticsearchClientConfig.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.ELASTICSEARCH_REST_CLIENT);
    }

    @BuildStep
    AdditionalBeanBuildItem build() {
        return AdditionalBeanBuildItem.unremovableOf(ElasticsearchRestClientProducer.class);
    }

    @BuildStep
    void elasticsearchClientConfigSupport(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotations) {
        // add the @ElasticsearchClientConfig class otherwise it won't be registered as a qualifier
        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClass(ElasticsearchClientConfig.class).build());

        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(ELASTICSEARCH_CLIENT_CONFIG, DotNames.APPLICATION_SCOPED, false));
    }

    @BuildStep
    HealthBuildItem addHealthCheck(ElasticsearchBuildTimeConfig buildTimeConfig) {
        return new HealthBuildItem("io.quarkus.elasticsearch.restclient.lowlevel.runtime.health.ElasticsearchHealthCheck",
                buildTimeConfig.healthEnabled);
    }

}
