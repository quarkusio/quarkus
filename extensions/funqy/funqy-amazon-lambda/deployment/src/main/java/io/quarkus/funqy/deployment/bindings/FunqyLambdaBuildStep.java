package io.quarkus.funqy.deployment.bindings;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.quarkus.amazon.lambda.deployment.LambdaObjectMapperInitializedBuildItem;
import io.quarkus.amazon.lambda.runtime.LambdaBuildTimeConfig;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.builder.BuildException;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
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

    @BuildStep()
    @Record(STATIC_INIT)
    public void init(List<FunctionBuildItem> functions,
            FunqyLambdaBindingRecorder recorder,
            Optional<FunctionInitializedBuildItem> hasFunctions,
            LambdaObjectMapperInitializedBuildItem mapperDependency,
            BeanContainerBuildItem beanContainer,
            FunqyConfig knative) throws Exception {
        if (!hasFunctions.isPresent() || hasFunctions.get() == null)
            return;

        String function = null;
        if (knative.export.isPresent()) {
            function = knative.export.get();
            boolean found = false;
            for (FunctionBuildItem funq : functions) {
                String matchName = funq.getFunctionName() == null ? funq.getMethodName() : funq.getFunctionName();
                if (function.equals(matchName)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new BuildException("Cannot find function specified by quarkus.funqy.knative.export ",
                        Collections.emptyList());

            }

        } else if (functions.size() == 1) {
            function = functions.get(0).getFunctionName();
            if (function == null) {
                function = functions.get(0).getMethodName();
            }
        } else {
            throw new BuildException("Too many functions in deployment, use quarkus.funqy.knative.export to narrow it",
                    Collections.emptyList());
        }
        recorder.init(beanContainer.getValue(), function);
    }

    /**
     * This should only run when building a native image
     */
    @BuildStep(onlyIf = NativeBuild.class)
    @Record(value = ExecutionTime.RUNTIME_INIT)
    void startPoolLoop(FunqyLambdaBindingRecorder recorder,
            ShutdownContextBuildItem shutdownContextBuildItem,
            List<ServiceStartBuildItem> orderServicesFirst // try to order this after service recorders
    ) {
        recorder.startPollLoop(shutdownContextBuildItem);
    }

    @BuildStep
    @Record(value = ExecutionTime.RUNTIME_INIT)
    void enableNativeEventLoop(LambdaBuildTimeConfig config,
            FunqyLambdaBindingRecorder recorder,
            List<ServiceStartBuildItem> orderServicesFirst, // force some ordering of recorders
            ShutdownContextBuildItem shutdownContextBuildItem,
            LaunchModeBuildItem launchModeBuildItem) {
        LaunchMode mode = launchModeBuildItem.getLaunchMode();
        if (config.enablePollingJvmMode && mode.isDevOrTest()) {
            recorder.startPollLoop(shutdownContextBuildItem);
        }
    }

}
