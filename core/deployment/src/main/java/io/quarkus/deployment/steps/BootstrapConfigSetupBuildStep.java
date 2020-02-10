package io.quarkus.deployment.steps;

import static io.quarkus.deployment.configuration.RunTimeConfigurationGenerator.C_CREATE_BOOTSTRAP_CONFIG;

import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.BootstrapConfigSetupCompleteBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.MainBytecodeRecorderBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.runtime.StartupContext;
import io.quarkus.runtime.StartupTask;

public class BootstrapConfigSetupBuildStep {

    private static final String BOOTSTRAP_CONFIG_STARTUP_TASK_CLASS_NAME = "io.quarkus.deployment.steps.BootstrapConfigSetup";

    /**
     * Generates a StartupTask that creates a instance of the generated Config class
     * It runs before any StartupTask that uses configuration
     */
    @BuildStep
    @Produce(BootstrapConfigSetupCompleteBuildItem.class)
    void setupBootstrapConfig(BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<MainBytecodeRecorderBuildItem> mainBytecodeRecorder) {
        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClass, true);

        try (ClassCreator clazz = ClassCreator.builder().classOutput(classOutput)
                .className(BOOTSTRAP_CONFIG_STARTUP_TASK_CLASS_NAME)
                .interfaces(StartupTask.class).build()) {

            try (MethodCreator deploy = clazz.getMethodCreator("deploy", void.class, StartupContext.class)) {
                deploy.invokeStaticMethod(C_CREATE_BOOTSTRAP_CONFIG);
                deploy.returnValue(null);
            }
        }

        mainBytecodeRecorder.produce(new MainBytecodeRecorderBuildItem(BOOTSTRAP_CONFIG_STARTUP_TASK_CLASS_NAME));
    }
}
