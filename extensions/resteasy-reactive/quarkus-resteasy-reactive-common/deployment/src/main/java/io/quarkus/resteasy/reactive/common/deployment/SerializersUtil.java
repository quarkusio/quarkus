package io.quarkus.resteasy.reactive.common.deployment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.RuntimeType;

import org.jboss.resteasy.reactive.common.core.Serialisers;
import org.jboss.resteasy.reactive.common.model.ResourceReader;
import org.jboss.resteasy.reactive.common.model.ResourceWriter;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.resteasy.reactive.common.runtime.ResteasyReactiveCommonRecorder;
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

        Map<String, MessageBodyReaderWriterOverrideData> writerOverrides = new HashMap<>();
        for (MessageBodyWriterOverrideBuildItem writerOverride : messageBodyWriterOverrideBuildItems) {
            writerOverrides.put(writerOverride.getClassName(), writerOverride.getOverrideData());
        }
        for (MessageBodyWriterBuildItem additionalWriter : RuntimeTypeItem.filter(messageBodyWriterBuildItems,
                runtimeType)) {
            ResourceWriter writer = new ResourceWriter();
            String writerClassName = additionalWriter.getClassName();
            MessageBodyReaderWriterOverrideData overrideData = writerOverrides.get(writerClassName);
            if (overrideData != null) {
                writer.setBuiltin(overrideData.isBuiltIn());
            } else {
                writer.setBuiltin(additionalWriter.isBuiltin());
            }
            writer.setFactory(FactoryUtils.factory(writerClassName,
                    applicationResultBuildItem.getResult().getSingletonClasses(), recorder,
                    beanContainerBuildItem));
            writer.setConstraint(additionalWriter.getRuntimeType());
            if (!additionalWriter.getMediaTypeStrings().isEmpty()) {
                writer.setMediaTypeStrings(additionalWriter.getMediaTypeStrings());
            }
            if (overrideData != null) {
                writer.setPriority(overrideData.getPriority());
            } else {
                writer.setPriority(additionalWriter.getPriority());
            }
            recorder.registerWriter(serialisers, additionalWriter.getHandledClassName(), writer);
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, false, writerClassName));
        }

        Map<String, MessageBodyReaderWriterOverrideData> readerOverrides = new HashMap<>();
        for (MessageBodyReaderOverrideBuildItem readerOverride : messageBodyReaderOverrideBuildItems) {
            readerOverrides.put(readerOverride.getClassName(), readerOverride.getOverrideData());
        }
        for (MessageBodyReaderBuildItem additionalReader : RuntimeTypeItem.filter(messageBodyReaderBuildItems,
                runtimeType)) {
            ResourceReader reader = new ResourceReader();
            String readerClassName = additionalReader.getClassName();
            MessageBodyReaderWriterOverrideData overrideData = readerOverrides.get(readerClassName);
            if (overrideData != null) {
                reader.setBuiltin(overrideData.isBuiltIn());
            } else {
                reader.setBuiltin(additionalReader.isBuiltin());
            }
            reader.setFactory(FactoryUtils.factory(readerClassName,
                    applicationResultBuildItem.getResult().getSingletonClasses(), recorder,
                    beanContainerBuildItem));
            reader.setConstraint(additionalReader.getRuntimeType());
            if (!additionalReader.getMediaTypeStrings().isEmpty()) {
                reader.setMediaTypeStrings(additionalReader.getMediaTypeStrings());
            }
            if (overrideData != null) {
                reader.setPriority(overrideData.getPriority());
            } else {
                reader.setPriority(additionalReader.getPriority());
            }
            recorder.registerReader(serialisers, additionalReader.getHandledClassName(), reader);
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, false, readerClassName));
        }

    }
}
