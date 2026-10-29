package io.quarkus.resteasy.reactive.common.deployment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.RuntimeType;

import org.jboss.resteasy.reactive.common.core.Serialisers;
import org.jboss.resteasy.reactive.common.model.ResourceReader;
import org.jboss.resteasy.reactive.common.model.ResourceWriter;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.resteasy.reactive.common.runtime.ResteasyReactiveCommonRecorder;
import io.quarkus.resteasy.reactive.common.runtime.SerializerDefinition;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderOverrideBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderWriterOverrideData;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterOverrideBuildItem;
import io.quarkus.resteasy.reactive.spi.RuntimeTypeItem;

public class SerializersUtil {

    public static void setupSerializers(ResteasyReactiveCommonRecorder recorder,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            List<MessageBodyReaderBuildItem> messageBodyReaderBuildItems,
            List<MessageBodyWriterBuildItem> messageBodyWriterBuildItems,
            List<MessageBodyReaderOverrideBuildItem> messageBodyReaderOverrideBuildItems,
            List<MessageBodyWriterOverrideBuildItem> messageBodyWriterOverrideBuildItems,
            BeanContainerBuildItem beanContainerBuildItem,
            ApplicationResultBuildItem applicationResultBuildItem,
            Serialisers serialisers, RuntimeType runtimeType) {

        Set<String> singletonClasses = applicationResultBuildItem.getResult().getSingletonClasses();

        for (SerializerDefinition definition : writerDefinitions(reflectiveClass, messageBodyWriterBuildItems,
                messageBodyWriterOverrideBuildItems, singletonClasses, runtimeType)) {
            ResourceWriter writer = new ResourceWriter();
            writer.setBuiltin(definition.builtin());
            writer.setFactory(FactoryUtils.factory(definition.className(), singletonClasses, recorder,
                    beanContainerBuildItem));
            writer.setConstraint(definition.constraint());
            if (!definition.mediaTypeStrings().isEmpty()) {
                writer.setMediaTypeStrings(definition.mediaTypeStrings());
            }
            writer.setPriority(definition.priority());
            recorder.registerWriter(serialisers, definition.handledClassName(), writer);
        }

        for (SerializerDefinition definition : readerDefinitions(reflectiveClass, messageBodyReaderBuildItems,
                messageBodyReaderOverrideBuildItems, singletonClasses, runtimeType)) {
            ResourceReader reader = new ResourceReader();
            reader.setBuiltin(definition.builtin());
            reader.setFactory(FactoryUtils.factory(definition.className(), singletonClasses, recorder,
                    beanContainerBuildItem));
            reader.setConstraint(definition.constraint());
            if (!definition.mediaTypeStrings().isEmpty()) {
                reader.setMediaTypeStrings(definition.mediaTypeStrings());
            }
            reader.setPriority(definition.priority());
            recorder.registerReader(serialisers, definition.handledClassName(), reader);
        }
    }

    /**
     * Describes the writers of the given runtime type, applying the overrides, and registers the writer classes for
     * reflection.
     */
    public static List<SerializerDefinition> writerDefinitions(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            List<MessageBodyWriterBuildItem> messageBodyWriterBuildItems,
            List<MessageBodyWriterOverrideBuildItem> messageBodyWriterOverrideBuildItems,
            Set<String> singletonClasses, RuntimeType runtimeType) {
        Map<String, MessageBodyReaderWriterOverrideData> writerOverrides = new HashMap<>();
        for (MessageBodyWriterOverrideBuildItem writerOverride : messageBodyWriterOverrideBuildItems) {
            writerOverrides.put(writerOverride.getClassName(), writerOverride.getOverrideData());
        }
        List<SerializerDefinition> definitions = new ArrayList<>();
        for (MessageBodyWriterBuildItem additionalWriter : RuntimeTypeItem.filter(messageBodyWriterBuildItems,
                runtimeType)) {
            String writerClassName = additionalWriter.getClassName();
            definitions.add(definition(writerClassName, additionalWriter.getHandledClassName(),
                    additionalWriter.getMediaTypeStrings(), additionalWriter.getRuntimeType(), additionalWriter.isBuiltin(),
                    additionalWriter.getPriority(), writerOverrides.get(writerClassName), singletonClasses));
            reflectiveClass.produce(
                    ReflectiveClassBuildItem.builder(writerClassName).build());
        }
        return definitions;
    }

    /**
     * Describes the readers of the given runtime type, applying the overrides, and registers the reader classes for
     * reflection.
     */
    public static List<SerializerDefinition> readerDefinitions(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            List<MessageBodyReaderBuildItem> messageBodyReaderBuildItems,
            List<MessageBodyReaderOverrideBuildItem> messageBodyReaderOverrideBuildItems,
            Set<String> singletonClasses, RuntimeType runtimeType) {
        Map<String, MessageBodyReaderWriterOverrideData> readerOverrides = new HashMap<>();
        for (MessageBodyReaderOverrideBuildItem readerOverride : messageBodyReaderOverrideBuildItems) {
            readerOverrides.put(readerOverride.getClassName(), readerOverride.getOverrideData());
        }
        List<SerializerDefinition> definitions = new ArrayList<>();
        for (MessageBodyReaderBuildItem additionalReader : RuntimeTypeItem.filter(messageBodyReaderBuildItems,
                runtimeType)) {
            String readerClassName = additionalReader.getClassName();
            definitions.add(definition(readerClassName, additionalReader.getHandledClassName(),
                    additionalReader.getMediaTypeStrings(), additionalReader.getRuntimeType(), additionalReader.isBuiltin(),
                    additionalReader.getPriority(), readerOverrides.get(readerClassName), singletonClasses));
            reflectiveClass.produce(
                    ReflectiveClassBuildItem.builder(readerClassName).build());
        }
        return definitions;
    }

    private static SerializerDefinition definition(String className, String handledClassName, List<String> mediaTypeStrings,
            RuntimeType runtimeType, boolean builtin, Integer priority, MessageBodyReaderWriterOverrideData overrideData,
            Set<String> singletonClasses) {
        if (overrideData != null) {
            builtin = overrideData.isBuiltIn();
            priority = overrideData.getPriority();
        }
        return new SerializerDefinition(className, handledClassName, List.copyOf(mediaTypeStrings), runtimeType, builtin,
                priority, singletonClasses.contains(className));
    }
}
