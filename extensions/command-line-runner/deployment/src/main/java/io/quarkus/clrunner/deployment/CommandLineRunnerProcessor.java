package io.quarkus.clrunner.deployment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.clrunner.CommandLineRunner;
import io.quarkus.clrunner.runtime.CommandLineRunnerTemplate;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.MainArgsBuildItem;
import io.quarkus.deployment.builditem.ShutdownBuildItem;
import io.quarkus.deployment.recording.RecorderContext;

public class CommandLineRunnerProcessor {

    private static final DotName COMMAND_LINER_RUNNER = DotName.createSimple(CommandLineRunner.class.getName());

    @BuildStep
    List<CommandLineRunnerBuildItem> discover(CombinedIndexBuildItem combinedIndexBuildItem) {
        final List<CommandLineRunnerBuildItem> result = new ArrayList<>();

        for (ClassInfo info : combinedIndexBuildItem.getIndex().getAllKnownImplementors(COMMAND_LINER_RUNNER)) {
            final DotName name = info.name();

            result.add(new CommandLineRunnerBuildItem(name.toString()));
        }
        return result;
    }

    @BuildStep
    List<AdditionalBeanBuildItem> beans(List<CommandLineRunnerBuildItem> runners) {
        final List<AdditionalBeanBuildItem> result = new ArrayList<>();
        for (CommandLineRunnerBuildItem runner : runners) {
            result.add(new AdditionalBeanBuildItem(false, runner.getClassName()));
        }
        return result;
    }

    @BuildStep
    ShutdownBuildItem shutdown() {
        // TODO: this has to be conditional only set when various things are not present
        return new ShutdownBuildItem();
    }

    @BuildStep
    @Record(ExecutionTime.AFTER_STARTUP)
    public void runners(List<CommandLineRunnerBuildItem> runners,
            BeanContainerBuildItem beanContainerBuildItem,
            MainArgsBuildItem mainArgsBuildItem,
            CommandLineRunnerTemplate template,
            RecorderContext context) {

        Set<Class<? extends CommandLineRunner>> runnerClasses = new HashSet<>();
        for (CommandLineRunnerBuildItem runner : runners) {
            runnerClasses.add((Class<? extends CommandLineRunner>) context.classProxy(runner.getClassName()));
        }
        template.run(runnerClasses, beanContainerBuildItem.getValue(), mainArgsBuildItem);
    }
}
