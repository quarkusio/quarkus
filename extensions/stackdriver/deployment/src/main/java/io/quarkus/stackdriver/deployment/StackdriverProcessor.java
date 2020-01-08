package io.quarkus.stackdriver.deployment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.resteasy.common.deployment.ResteasyInjectionReadyBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.stackdriver.SpanInterceptor;
import io.quarkus.stackdriver.runtime.SpanPropagationClientRequestFilter;
import io.quarkus.stackdriver.runtime.StackdriverConfigurer;
import io.quarkus.stackdriver.runtime.StackdriverProducer;
import io.quarkus.stackdriver.runtime.StackdriverRecorder;
import io.quarkus.stackdriver.runtime.StackdriverTracingStandaloneVertxDynamicFeature;
import io.quarkus.stackdriver.runtime.configuration.StackdriverConfiguration;

public class StackdriverProcessor {

    private static final String FEATURE = "gcp.stackdriver";

    private static Logger LOGGER = LoggerFactory.getLogger(StackdriverProcessor.class);

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    public void registerBeans(
            BuildProducer<StackdriverInitiliazedBuildItem> stackdriverInitializedBuildItem,
            ResteasyInjectionReadyBuildItem resteasyInjectionReadyBuildItem,
            StackdriverRecorder stackdriverRecorder,
            StackdriverConfiguration stackdriverConfiguration) {
        stackdriverRecorder.runtimeConfiguration(stackdriverConfiguration);
        stackdriverInitializedBuildItem.produce(new StackdriverInitiliazedBuildItem());
    }

    @BuildStep
    public void setupFilter(BuildProducer<ResteasyJaxrsProviderBuildItem> providers,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(FEATURE));
        providers.produce(new ResteasyJaxrsProviderBuildItem(StackdriverTracingStandaloneVertxDynamicFeature.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(SpanPropagationClientRequestFilter.class.getName()));
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClasses(StackdriverConfigurer.class)
                .addBeanClasses(StackdriverProducer.class)
                .addBeanClasses(SpanInterceptor.class)
                .setUnremovable()
                .build());
    }

}
