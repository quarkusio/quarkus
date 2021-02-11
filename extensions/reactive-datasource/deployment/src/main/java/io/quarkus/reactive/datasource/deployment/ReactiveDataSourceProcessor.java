package io.quarkus.reactive.datasource.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.reactive.datasource.ReactiveDataSource;

class ReactiveDataSourceProcessor {

    @BuildStep
    void addQualifierAsBean(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        // add the @ReactiveDataSource class otherwise it won't be registered as a qualifier
        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClass(ReactiveDataSource.class).build());
    }
}
