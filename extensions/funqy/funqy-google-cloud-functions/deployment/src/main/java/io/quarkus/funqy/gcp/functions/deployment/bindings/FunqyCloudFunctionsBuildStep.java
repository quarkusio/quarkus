package io.quarkus.funqy.gcp.functions.deployment.bindings;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.funqy.deployment.FunctionBuildItem;
import io.quarkus.funqy.deployment.FunctionInitializedBuildItem;
import io.quarkus.funqy.gcp.functions.FunqyCloudFunctionsBindingRecorder;
import io.quarkus.jackson.runtime.ObjectMapperProducer;

public class FunqyCloudFunctionsBuildStep {
    private static final String FEATURE_NAME = "funqy-google-cloud-functions";

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE_NAME);
    }

    @BuildStep
    public RunTimeConfigurationDefaultBuildItem disableBanner() {
        // the banner is not displayed well inside the Google Cloud Function logs
        return new RunTimeConfigurationDefaultBuildItem("quarkus.banner.enabled", "false");
    }

    @BuildStep
    @Record(STATIC_INIT)
    public void init(List<FunctionBuildItem> functions,
            FunqyCloudFunctionsBindingRecorder recorder,
            Optional<FunctionInitializedBuildItem> hasFunctions,
            BeanContainerBuildItem beanContainer) {
        if (!hasFunctions.isPresent())
            return;

        recorder.init(beanContainer.getValue());
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public void choose(FunqyCloudFunctionsBindingRecorder recorder) {
        recorder.chooseInvoker();
    }

    @BuildStep
    public void markObjectMapperUnremovable(BuildProducer<UnremovableBeanBuildItem> unremovable) {
        unremovable.produce(new UnremovableBeanBuildItem(
                new UnremovableBeanBuildItem.BeanClassNameExclusion(ObjectMapper.class.getName())));
        unremovable.produce(new UnremovableBeanBuildItem(
                new UnremovableBeanBuildItem.BeanClassNameExclusion(ObjectMapperProducer.class.getName())));
    }
}
