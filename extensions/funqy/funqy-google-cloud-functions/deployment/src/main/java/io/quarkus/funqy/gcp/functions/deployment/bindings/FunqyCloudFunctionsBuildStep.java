package io.quarkus.funqy.gcp.functions.deployment.bindings;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.List;
import java.util.Optional;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.funqy.deployment.FunctionBuildItem;
import io.quarkus.funqy.deployment.FunctionInitializedBuildItem;
import io.quarkus.funqy.gcp.functions.FunqyCloudFunctionsBindingRecorder;
import io.quarkus.funqy.runtime.FunqyConfig;

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
            BeanContainerBuildItem beanContainer) throws Exception {
        if (!hasFunctions.isPresent() || hasFunctions.get() == null)
            return;

        recorder.init(beanContainer.getValue());
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public void choose(FunqyConfig config, FunqyCloudFunctionsBindingRecorder recorder) {
        recorder.chooseInvoker(config);
    }
}
