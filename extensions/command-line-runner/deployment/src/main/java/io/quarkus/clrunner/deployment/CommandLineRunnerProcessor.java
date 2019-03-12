package io.quarkus.clrunner.deployment;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.clrunner.CommandLineRunner;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.MainAfterStartupBuildItem;
import io.quarkus.deployment.builditem.ShutdownBuildItem;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

public class CommandLineRunnerProcessor {

    private static final DotName COMMAND_LINER_RUNNER = DotName.createSimple(CommandLineRunner.class.getName());

    @BuildStep
    void discover(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<CommandLineRunnerBuildItem> producer) {
        final List<String> names = new ArrayList<>();

        for (ClassInfo info : combinedIndexBuildItem.getIndex().getAllKnownImplementors(COMMAND_LINER_RUNNER)) {
            final DotName name = info.name();
            names.add(name.toString());
        }

        if (names.size() == 0) {
            return;
        }
        if (names.size() > 1) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (String name : names) {
                if (first) {
                    first = false;
                } else {
                    sb.append(",");
                }
                sb.append(name);
            }
            throw new RuntimeException("Multiple classes ( " + sb.toString()
                    + ") have been annotated with @ApplicationPath which is currently not supported");
        }

        producer.produce(new CommandLineRunnerBuildItem(names.get(0)));
    }

    @BuildStep
    void beans(Optional<CommandLineRunnerBuildItem> runner, BuildProducer<AdditionalBeanBuildItem> producer) {
        if (runner.isPresent()) {
            producer.produce(new AdditionalBeanBuildItem(false, runner.get().getClassName()));
        }
    }

    @BuildStep
    void shutdown(Optional<CommandLineRunnerBuildItem> runner, BuildProducer<ShutdownBuildItem> producer) {
        // TODO: this has to be conditional only set when various things are not present
        if (runner.isPresent()) {
            producer.produce(new ShutdownBuildItem());
        }
    }

    @BuildStep
    void bytecodeCreation(Optional<CommandLineRunnerBuildItem> bi, BuildProducer<MainAfterStartupBuildItem> producer) {
        if (bi.isPresent()) {
            final Consumer<MainAfterStartupBuildItem.Input> bytecodeCreator = new Consumer<MainAfterStartupBuildItem.Input>() {
                @Override
                public void accept(MainAfterStartupBuildItem.Input input) {
                    MethodCreator mv = input.getDoStartMethod();
                    ResultHandle clazz = mv.invokeStaticMethod(
                            ofMethod(Class.class, "forName", Class.class, String.class),
                            mv.load(bi.get().getClassName()));
                    ResultHandle arcContainer = mv.invokeStaticMethod(
                            MethodDescriptor.ofMethod(Arc.class, "container", ArcContainer.class));
                    ResultHandle instanceHandle = mv.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(ArcContainer.class, "instance", InstanceHandle.class, Class.class,
                                    Annotation[].class),
                            arcContainer, clazz, mv.newArray(Annotation.class, mv.load(0)));
                    ResultHandle runner = mv.invokeInterfaceMethod(
                            ofMethod(InstanceHandle.class, "get", Object.class),
                            instanceHandle);
                    mv.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(CommandLineRunner.class, "run", void.class, String[].class),
                            runner, input.getMainArgs());
                }
            };

            producer.produce(new MainAfterStartupBuildItem(bytecodeCreator));
        }
    }
}
