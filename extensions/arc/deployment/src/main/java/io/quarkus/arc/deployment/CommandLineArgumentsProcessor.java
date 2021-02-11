package io.quarkus.arc.deployment;

import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.RawCommandLineArgumentsBuildItem;
import io.quarkus.runtime.annotations.CommandLineArguments;

public class CommandLineArgumentsProcessor {

    @BuildStep
    SyntheticBeanBuildItem commandLineArgs(RawCommandLineArgumentsBuildItem rawCommandLineArgumentsBuildItem,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(new AdditionalBeanBuildItem(CommandLineArguments.class));

        Type stringArray = Type.create(DotName.createSimple(String[].class.getName()), Kind.ARRAY);
        // implClazz is ignored because a provider type is set
        return SyntheticBeanBuildItem.configure(Object.class).providerType(stringArray).addType(stringArray)
                .addQualifier(CommandLineArguments.class)
                .setRuntimeInit().supplier(rawCommandLineArgumentsBuildItem)
                .unremovable().done();
    }

}
