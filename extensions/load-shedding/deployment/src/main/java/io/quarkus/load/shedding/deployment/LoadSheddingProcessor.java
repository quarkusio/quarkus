package io.quarkus.load.shedding.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.load.shedding.runtime.HttpLoadShedding;
import io.quarkus.load.shedding.runtime.HttpRequestClassifier;
import io.quarkus.load.shedding.runtime.LoadSheddingCounter;
import io.quarkus.load.shedding.runtime.ManagementRequestPrioritizer;
import io.quarkus.load.shedding.runtime.OverloadDetector;
import io.quarkus.load.shedding.runtime.PriorityLoadShedding;
import io.quarkus.runtime.metrics.MetricsFactory;

public class LoadSheddingProcessor {
    private static final String FEATURE = "load-shedding";

    // referenced by name, not as a class literal: the class implements `MeterBinder`, so
    // loading it here would also load Micrometer, which is exactly what must not happen
    // when the Micrometer extension is absent
    private static final String LOAD_SHEDDING_METRICS = "io.quarkus.load.shedding.runtime.LoadSheddingMetrics";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem beans() {
        List<String> beans = new ArrayList<>();
        beans.add(OverloadDetector.class.getName());
        beans.add(HttpLoadShedding.class.getName());
        beans.add(PriorityLoadShedding.class.getName());
        beans.add(ManagementRequestPrioritizer.class.getName());
        beans.add(HttpRequestClassifier.class.getName());
        beans.add(LoadSheddingCounter.class.getName());

        return AdditionalBeanBuildItem.builder().addBeanClasses(beans).build();
    }

    @BuildStep
    void metrics(LoadSheddingBuildTimeConfig config, Optional<MetricsCapabilityBuildItem> metricsCapability,
            BuildProducer<AdditionalBeanBuildItem> beans) {

        if (!config.metricsEnabled()) {
            return;
        }
        if (!metricsCapability.map(it -> it.metricsSupported(MetricsFactory.MICROMETER)).orElse(false)) {
            return;
        }

        beans.produce(AdditionalBeanBuildItem.builder().addBeanClass(LOAD_SHEDDING_METRICS).build());
    }
}
