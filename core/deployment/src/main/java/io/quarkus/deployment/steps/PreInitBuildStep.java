package io.quarkus.deployment.steps;

import java.lang.constant.ClassDesc;
import java.net.InetAddress;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.quarkus.deployment.GeneratedClassGizmo2Adaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.PreInitBuildItem;
import io.quarkus.deployment.builditem.PreInitRunnableBuildItem;
import io.quarkus.deployment.pkg.AotJarEnabled;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.runtime.init.AbstractPreInitRunner;
import io.quarkus.runtime.init.TimeZonePreInitRunnable;

public class PreInitBuildStep {

    private static final String PRE_INIT_RUNNER_CLASS_NAME = "io.quarkus.runtime.generated.PreInitRunner";
    private static final MethodDesc PRE_INIT_RUNNER_EXECUTE_PRE_INIT_TASKS_METHOD = ClassMethodDesc
            .of(ClassDesc.of(PRE_INIT_RUNNER_CLASS_NAME), "executePreInitTasks", void.class);
    public static final MethodDescriptor PRE_INIT_RUNNER_EXECUTE_PRE_INIT_TASKS_GIZMO1_METHOD = MethodDescriptor
            .ofMethod(PRE_INIT_RUNNER_CLASS_NAME, "executePreInitTasks", void.class);

    /**
     * For now, this is only used when using the AOT runner.
     * We might want to also use it for fast-jar at some point but let's be safe for now.
     */
    @BuildStep(onlyIf = AotJarEnabled.class)
    PreInitBuildItem executePreInitTasks(List<PreInitRunnableBuildItem> preInitRunnables,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<GeneratedResourceBuildItem> generatedResources) {
        ClassOutput output = new GeneratedClassGizmo2Adaptor(generatedClasses, generatedResources, false);
        Gizmo gizmo = Gizmo.create(output)
                .withDebugInfo(false)
                .withParameters(false);

        // we deduplicate the list, keeping the element with the lower priority in case of duplicate
        List<PreInitRunnableBuildItem> deduplicatedPreInitRunnables = preInitRunnables.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        Function.identity(),
                        (a, b) -> a.getPriority() <= b.getPriority() ? a : b))
                .values().stream().sorted().toList();

        gizmo.class_(PRE_INIT_RUNNER_CLASS_NAME, cc -> {
            cc.extends_(AbstractPreInitRunner.class);

            cc.staticMethod(PRE_INIT_RUNNER_EXECUTE_PRE_INIT_TASKS_METHOD, mc -> {
                mc.public_();
                mc.body(b0 -> {
                    Expr preinitRunnableArray = b0.newArray(Runnable.class, deduplicatedPreInitRunnables, pir -> {
                        ClassDesc runnableClassDesc = ClassDesc.of(pir.getRunnableClassName());
                        Expr runnableInstance;

                        // the model here is extremely naive and, if we need more parameters, we will need to make it more clever
                        // let's keep it simple for now
                        if (pir.getParameter() != null) {
                            runnableInstance = b0.new_(ConstructorDesc.of(runnableClassDesc, String.class),
                                    Const.of(pir.getParameter()));
                        } else {
                            runnableInstance = b0.new_(ConstructorDesc.of(runnableClassDesc));
                        }

                        return runnableInstance;
                    });

                    b0.invokeStatic(
                            MethodDesc.of(AbstractPreInitRunner.class, "doExecutePreInitTasks", void.class, Runnable[].class),
                            preinitRunnableArray);

                    b0.return_();
                });
            });
        });

        return new PreInitBuildItem();
    }

    @BuildStep
    void corePreInitTasks(BuildProducer<PreInitRunnableBuildItem> preInitTasks) {
        // DO NOT go berserk here: we should limit ourselves to very low level elements,
        // and element we cannot optimize by ourselves.
        // we also need to be careful to avoid circular reference, thus why we don't initialize LoggingProviders here

        // this is required by JBoss Logging and is rather costly
        preInitTasks.produce(PreInitRunnableBuildItem.runnable(TimeZonePreInitRunnable.class.getName()));

        // InetAddress is used in config and other places
        preInitTasks.produce(PreInitRunnableBuildItem.initializeClass(InetAddress.class.getName()));
    }
}
