package io.quarkus.claesh.deployment;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.inject.Singleton;

import org.aesh.command.CommandDefinition;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.claesh.AeshCommandRunner;
import io.quarkus.claesh.QuarkusCommand;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.MainAfterStartupBuildItem;
import io.quarkus.deployment.builditem.ShutdownBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

public class AeshCommandLineRunnerProcessor {

    private static final DotName QUARKUS_COMMAND = DotName.createSimple(QuarkusCommand.class.getName());
    private static final DotName SINGLETON = DotName.createSimple(Singleton.class.getName());
    private static final DotName COMMAND_DEFINITION = DotName.createSimple(CommandDefinition.class.getName());

    @BuildStep
    void discover(ApplicationIndexBuildItem indexBuildItem,
            BuildProducer<AeshCommandLineRunnerBuildItem> commandsProducer,
            BuildProducer<UnremovableBeanBuildItem> unremovableProducer) {

        for (ClassInfo info : indexBuildItem.getIndex().getAllKnownImplementors(QUARKUS_COMMAND)) {
            if (info.classAnnotation(COMMAND_DEFINITION) == null) {
                throw new RuntimeException("All implementations of " + QuarkusCommand.class.getName() +
                        " must be annotated with " + COMMAND_DEFINITION.toString());
            }
            commandsProducer.produce(new AeshCommandLineRunnerBuildItem(info.name().toString()));
        }

        // we need to make sure that all application Singleton beans are not removed
        Set<DotName> typesInjectedInCommands = new HashSet<>();
        // TODO: figure out if @Singleton is the only thing that makes sense in a Command Line application?
        for (AnnotationInstance annotationInstance : indexBuildItem.getIndex().getAnnotations(SINGLETON)) {
            typesInjectedInCommands.add(annotationInstance.target().asClass().name());
        }
        if (!typesInjectedInCommands.isEmpty()) {
            unremovableProducer.produce(new UnremovableBeanBuildItem(
                    new Predicate<BeanInfo>() {
                        @Override
                        public boolean test(BeanInfo beanInfo) {
                            Set<Type> types = beanInfo.getTypes();
                            for (Type t : types) {
                                if (typesInjectedInCommands.contains(t.name())) {
                                    return true;
                                }
                            }

                            return false;
                        }
                    }));
        }
    }

    @BuildStep
    List<ReflectiveClassBuildItem> reflection(List<AeshCommandLineRunnerBuildItem> commands) {
        final List<ReflectiveClassBuildItem> result = new ArrayList<>();
        for (AeshCommandLineRunnerBuildItem command : commands) {
            result.add(new ReflectiveClassBuildItem(true, true, command.getClassName()));
        }
        return result;
    }

    @BuildStep
    void shutdown(List<AeshCommandLineRunnerBuildItem> commands, BuildProducer<ShutdownBuildItem> producer) {
        // TODO: this has to be conditional only set when various things are not present
        if (!commands.isEmpty()) {
            producer.produce(new ShutdownBuildItem());
        }
    }

    @BuildStep
    void bytecodeCreation(List<AeshCommandLineRunnerBuildItem> commands, BuildProducer<MainAfterStartupBuildItem> producer) {
        if (commands.isEmpty()) {
            return;
        }

        // generate the bytecode that will call AeshCommandRunner with the proper parameters
        final Consumer<MainAfterStartupBuildItem.Input> bytecodeCreator = new Consumer<MainAfterStartupBuildItem.Input>() {
            @Override
            public void accept(MainAfterStartupBuildItem.Input input) {
                MethodCreator mv = input.getDoStartMethod();

                ResultHandle commandsArray = mv.newArray(Class.class, mv.load(commands.size()));

                for (int i = 0; i < commands.size(); i++) {
                    ResultHandle clazz = mv.invokeStaticMethod(
                            ofMethod(Class.class, "forName", Class.class, String.class),
                            mv.load(commands.get(i).getClassName()));
                    mv.writeArrayValue(commandsArray, i, clazz);
                }

                final ResultHandle runner = mv.newInstance(MethodDescriptor.ofConstructor(AeshCommandRunner.class));

                mv.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(AeshCommandRunner.class, "run", void.class, Class[].class, String[].class),
                        runner, commandsArray, input.getMainArgs());
            }
        };

        producer.produce(new MainAfterStartupBuildItem(bytecodeCreator));
    }
}
