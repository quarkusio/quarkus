package io.quarkus.deployment.steps;

import static io.quarkus.deployment.configuration.RunTimeConfigurationGenerator.C_CREATE_RUN_TIME_CONFIG;
import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.MainBytecodeRecorderBuildItem;
import io.quarkus.deployment.builditem.RuntimeConfigSetupCompleteBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.runtime.StartupContext;
import io.quarkus.runtime.StartupTask;

public class RuntimeConfigSetupBuildStep {
    private static final String RUNTIME_CONFIG_STARTUP_TASK_CLASS_NAME = "io.quarkus.deployment.steps.RuntimeConfigSetup";

    /**
     * Generates a StartupTask that sets up the final runtime configuration and thus runs before any StartupTask that uses
     * runtime configuration.
     * If there are recorders that produce a ConfigSourceProvider, these objects are used to set up the final runtime
     * configuration
     */
    @BuildStep
    @Produce(RuntimeConfigSetupCompleteBuildItem.class)
    void setupRuntimeConfig(
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<MainBytecodeRecorderBuildItem> mainBytecodeRecorder) {
        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClass, true);

        try (ClassCreator clazz = ClassCreator.builder().classOutput(classOutput)
                .className(RUNTIME_CONFIG_STARTUP_TASK_CLASS_NAME)
                .interfaces(StartupTask.class).build()) {

            try (MethodCreator method = clazz.getMethodCreator("deploy", void.class, StartupContext.class)) {
                method.invokeVirtualMethod(ofMethod(StartupContext.class, "setCurrentBuildStepName", void.class, String.class),
                        method.getMethodParam(0), method.load("RuntimeConfigSetupBuildStep.setupRuntimeConfig"));

                method.invokeStaticMethod(C_CREATE_RUN_TIME_CONFIG);
                method.returnValue(null);
            }
        }

        mainBytecodeRecorder.produce(new MainBytecodeRecorderBuildItem(RUNTIME_CONFIG_STARTUP_TASK_CLASS_NAME));
    }
}
