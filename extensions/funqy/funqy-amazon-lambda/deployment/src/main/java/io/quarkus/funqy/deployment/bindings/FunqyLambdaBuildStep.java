package io.quarkus.funqy.deployment.bindings;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.List;
import java.util.Optional;

import io.quarkus.amazon.lambda.deployment.LambdaObjectMapperInitializedBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.funqy.deployment.FunctionBuildItem;
import io.quarkus.funqy.deployment.FunctionInitializedBuildItem;
import io.quarkus.funqy.lambda.FunqyLambdaBindingRecorder;
import io.quarkus.funqy.runtime.FunqyConfig;
import io.quarkus.runtime.LaunchMode;

public class FunqyLambdaBuildStep {

    public static final String FUNQY_AMAZON_LAMBDA = "funqy-amazon-lambda";

    public static final class RuntimeComplete extends SimpleBuildItem {
    }

    @BuildStep()
    @Record(STATIC_INIT)
    public void init(List<FunctionBuildItem> functions,
            FunqyLambdaBindingRecorder recorder,
            BuildProducer<FeatureBuildItem> feature,
            Optional<FunctionInitializedBuildItem> hasFunctions,
            LambdaObjectMapperInitializedBuildItem mapperDependency,
            BeanContainerBuildItem beanContainer) throws Exception {
        if (!hasFunctions.isPresent() || hasFunctions.get() == null)
            return;
        feature.produce(new FeatureBuildItem(FUNQY_AMAZON_LAMBDA));
        recorder.init(beanContainer.getValue());
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public RuntimeComplete choose(FunqyConfig config, FunqyLambdaBindingRecorder recorder) {
        recorder.chooseInvoker(config);
        return new RuntimeComplete();
    }

    /**
     * This should only run when building a native image
     */
    @BuildStep(onlyIf = NativeBuild.class)
    @Record(RUNTIME_INIT)
    public void startPoolLoop(FunqyLambdaBindingRecorder recorder,
            RuntimeComplete ignored,
            ShutdownContextBuildItem shutdownContextBuildItem,
            List<ServiceStartBuildItem> orderServicesFirst // try to order this after service recorders
    ) {
        recorder.startPollLoop(shutdownContextBuildItem);
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public void startPoolLoopDevOrTest(RuntimeComplete ignored,
            FunqyLambdaBindingRecorder recorder,
            List<ServiceStartBuildItem> orderServicesFirst, // force some ordering of recorders
            ShutdownContextBuildItem shutdownContextBuildItem,
            LaunchModeBuildItem launchModeBuildItem) {
        LaunchMode mode = launchModeBuildItem.getLaunchMode();
        if (mode.isDevOrTest()) {
            recorder.startPollLoop(shutdownContextBuildItem);
        }
    }
}
