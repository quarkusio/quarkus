package io.quarkus.deployment.steps;

import static io.quarkus.deployment.configuration.RunTimeConfigurationGenerator.C_CREATE_BOOTSTRAP_CONFIG;

import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.BootstrapConfigSetupCompleteBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.runtime.BootstrapConfigRecorder;
import io.quarkus.runtime.StartupTask;

public class BootstrapConfigSetupBuildStep {

    private static final String BOOTSTRAP_CONFIG_STARTUP_TASK_CLASS_NAME = "io.quarkus.deployment.steps.BootstrapConfigSetup";

    /**
     * Generates a StartupTask that creates a instance of the generated Config class
     * It runs before any StartupTask that uses configuration
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    @Produce(BootstrapConfigSetupCompleteBuildItem.class)
    void setupBootstrapConfig(BuildProducer<GeneratedClassBuildItem> generatedClass,
            RecorderContext rc,
            BootstrapConfigRecorder recorder) {
        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClass, true);

        try (ClassCreator clazz = ClassCreator.builder().classOutput(classOutput)
                .className(BOOTSTRAP_CONFIG_STARTUP_TASK_CLASS_NAME)
                .interfaces(Runnable.class).build()) {

            try (MethodCreator deploy = clazz.getMethodCreator("run", void.class)) {
                deploy.invokeStaticMethod(C_CREATE_BOOTSTRAP_CONFIG);
                deploy.returnValue(null);
            }
        }
        recorder.run(rc.newInstance(BOOTSTRAP_CONFIG_STARTUP_TASK_CLASS_NAME));
    }
}
