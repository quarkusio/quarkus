package io.quarkus.deployment.steps;

import static io.quarkus.deployment.configuration.RunTimeConfigurationGenerator.C_INSTANCE;
import static io.quarkus.gizmo.MethodDescriptor.ofConstructor;
import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.jboss.logging.Logger;

import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.BootstrapConfigSetupCompleteBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.MainBytecodeRecorderBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationSourceValueBuildItem;
import io.quarkus.deployment.builditem.RuntimeConfigSetupCompleteBuildItem;
import io.quarkus.deployment.configuration.RunTimeConfigurationGenerator;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.StartupContext;
import io.quarkus.runtime.StartupTask;

public class RuntimeConfigSetupBuildStep {

    private static final Logger log = Logger.getLogger(ReflectiveHierarchyStep.class);

    private static final String RUNTIME_CONFIG_STARTUP_TASK_CLASS_NAME = "io.quarkus.deployment.steps.RuntimeConfigSetup";

    /**
     * Generates a StartupTask that sets up the final runtime configuration and thus runs before any StartupTask that uses
     * runtime configuration.
     * If there are recorders that produce a ConfigSourceProvider, these objects are used to set up the final runtime
     * configuration
     */
    @BuildStep
    @Consume(BootstrapConfigSetupCompleteBuildItem.class)
    @Produce(RuntimeConfigSetupCompleteBuildItem.class)
    void setupRuntimeConfig(List<RunTimeConfigurationSourceValueBuildItem> runTimeConfigurationSourceValues,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<MainBytecodeRecorderBuildItem> mainBytecodeRecorder) {
        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClass, true);

        try (ClassCreator clazz = ClassCreator.builder().classOutput(classOutput)
                .className(RUNTIME_CONFIG_STARTUP_TASK_CLASS_NAME)
                .interfaces(StartupTask.class).build()) {

            try (MethodCreator method = clazz.getMethodCreator("deploy", void.class, StartupContext.class)) {
                ResultHandle config = method.readStaticField(C_INSTANCE);

                if (runTimeConfigurationSourceValues.isEmpty()) {
                    method.invokeVirtualMethod(RunTimeConfigurationGenerator.C_READ_CONFIG, config,
                            method.invokeStaticMethod(ofMethod(Collections.class, "emptyList", List.class)));
                } else {
                    ResultHandle startupContext = method.getMethodParam(0);
                    ResultHandle configSourcesProvidersList = method.newInstance(ofConstructor(ArrayList.class, int.class),
                            method.load(runTimeConfigurationSourceValues.size()));
                    for (RunTimeConfigurationSourceValueBuildItem runTimeConfigurationSourceValue : runTimeConfigurationSourceValues) {
                        RuntimeValue<ConfigSourceProvider> runtimeValue = runTimeConfigurationSourceValue
                                .getConfigSourcesValue();
                        if (runtimeValue instanceof BytecodeRecorderImpl.ReturnedProxy) {
                            String proxyId = ((BytecodeRecorderImpl.ReturnedProxy) runtimeValue).__returned$proxy$key();
                            ResultHandle value = method.invokeVirtualMethod(
                                    ofMethod(StartupContext.class, "getValue", Object.class, String.class),
                                    startupContext, method.load(proxyId));
                            ResultHandle configSourceProvider = method
                                    .invokeVirtualMethod(ofMethod(RuntimeValue.class, "getValue", Object.class),
                                            method.checkCast(value, RuntimeValue.class));
                            method.invokeVirtualMethod(
                                    MethodDescriptor.ofMethod(ArrayList.class, "add", boolean.class, Object.class),
                                    configSourcesProvidersList,
                                    method.checkCast(configSourceProvider, ConfigSourceProvider.class));
                        } else {
                            log.warn("RuntimeValue " + runtimeValue
                                    + " was not produced by a recorder and it will thus be ignored");
                        }
                    }
                    method.invokeVirtualMethod(RunTimeConfigurationGenerator.C_READ_CONFIG, config,
                            configSourcesProvidersList);
                }

                method.returnValue(null);
            }
        }

        mainBytecodeRecorder.produce(new MainBytecodeRecorderBuildItem(RUNTIME_CONFIG_STARTUP_TASK_CLASS_NAME));
    }
}
