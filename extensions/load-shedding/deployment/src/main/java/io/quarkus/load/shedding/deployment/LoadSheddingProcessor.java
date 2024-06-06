package io.quarkus.load.shedding.deployment;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.load.shedding.runtime.HttpLoadShedding;
import io.quarkus.load.shedding.runtime.HttpRequestClassifier;
import io.quarkus.load.shedding.runtime.ManagementRequestPrioritizer;
import io.quarkus.load.shedding.runtime.OverloadDetector;
import io.quarkus.load.shedding.runtime.PriorityLoadShedding;

public class LoadSheddingProcessor {
    private static final String FEATURE = "load-shedding";

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

        return AdditionalBeanBuildItem.builder().addBeanClasses(beans).build();
    }
}
