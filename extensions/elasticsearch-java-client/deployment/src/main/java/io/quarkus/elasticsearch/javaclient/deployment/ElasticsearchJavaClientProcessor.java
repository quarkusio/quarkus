package io.quarkus.elasticsearch.javaclient.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.NativeImageFeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.elasticsearch.javaclient.runtime.ElasticsearchJavaClientProducer;

class ElasticsearchJavaClientProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.ELASTICSEARCH_JAVA_CLIENT);
    }

    @BuildStep
    AdditionalBeanBuildItem build() {
        return AdditionalBeanBuildItem.unremovableOf(ElasticsearchJavaClientProducer.class);
    }

    @BuildStep
    ServiceProviderBuildItem serviceProvider() {
        return new ServiceProviderBuildItem("jakarta.json.spi.JsonProvider",
                "co.elastic.clients.json.jackson.JacksonJsonProvider");
    }

    @BuildStep
    ReflectiveClassBuildItem jsonProvider() {
        return ReflectiveClassBuildItem.builder("org.eclipse.parsson.JsonProviderImpl").build();
    }

    @BuildStep
    NativeImageFeatureBuildItem enableElasticsearchJavaClientFeature() {
        return new NativeImageFeatureBuildItem(
                "io.quarkus.elasticsearch.javaclient.runtime.graalvm.ElasticsearchJavaClientFeature");
    }

}
