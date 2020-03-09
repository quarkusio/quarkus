package io.quarkus.arc.deployment;

import io.quarkus.arc.runtime.ArcRecorder;
import io.quarkus.arc.runtime.CommandLineArgumentsProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.RawCommandLineArgumentsBuildItem;
import io.quarkus.runtime.annotations.CommandLineArguments;

public class CommandLineArgumentsProcessor {

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    BeanContainerListenerBuildItem commandLineArgs(RawCommandLineArgumentsBuildItem rawCommandLineArgumentsBuildItem,
            ArcRecorder arcRecorder) {
        //todo: this should be filtered
        return new BeanContainerListenerBuildItem(arcRecorder.initCommandLineArgs(rawCommandLineArgumentsBuildItem));
    }

    @BuildStep
    AdditionalBeanBuildItem qualifier() {
        return AdditionalBeanBuildItem.builder().setUnremovable()
                .addBeanClasses(CommandLineArguments.class, CommandLineArgumentsProducer.class).build();
    }
}
