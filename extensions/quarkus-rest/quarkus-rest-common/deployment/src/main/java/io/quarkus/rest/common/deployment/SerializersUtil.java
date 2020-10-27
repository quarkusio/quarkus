package io.quarkus.rest.common.deployment;

import java.util.List;

import javax.ws.rs.RuntimeType;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.rest.common.runtime.QuarkusRestCommonRecorder;
import io.quarkus.rest.common.runtime.core.Serialisers;
import io.quarkus.rest.common.runtime.model.ResourceReader;
import io.quarkus.rest.common.runtime.model.ResourceWriter;
import io.quarkus.rest.spi.MessageBodyReaderBuildItem;
import io.quarkus.rest.spi.MessageBodyWriterBuildItem;
import io.quarkus.rest.spi.RuntimeTypeBuildItem;

public class SerializersUtil {

    public static void setupSerializers(QuarkusRestCommonRecorder recorder,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            List<MessageBodyReaderBuildItem> messageBodyReaderBuildItems,
            List<MessageBodyWriterBuildItem> messageBodyWriterBuildItems,
            BeanContainerBuildItem beanContainerBuildItem,
            ApplicationResultBuildItem applicationResultBuildItem,
            Serialisers serialisers, RuntimeType runtimeType) {

        for (MessageBodyWriterBuildItem additionalWriter : RuntimeTypeBuildItem.filter(messageBodyWriterBuildItems,
                runtimeType)) {
            ResourceWriter writer = new ResourceWriter();
            writer.setBuiltin(additionalWriter.isBuiltin());
            String writerClassName = additionalWriter.getClassName();
            writer.setFactory(FactoryUtils.factory(writerClassName, applicationResultBuildItem.getSingletonClasses(), recorder,
                    beanContainerBuildItem));
            writer.setConstraint(additionalWriter.getRuntimeType());
            if (!additionalWriter.getMediaTypeStrings().isEmpty()) {
                writer.setMediaTypeStrings(additionalWriter.getMediaTypeStrings());
            }
            recorder.registerWriter(serialisers, additionalWriter.getHandledClassName(), writer);
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, false, writerClassName));
        }
        for (MessageBodyReaderBuildItem additionalReader : RuntimeTypeBuildItem.filter(messageBodyReaderBuildItems,
                runtimeType)) {
            ResourceReader reader = new ResourceReader();
            reader.setBuiltin(false);
            String readerClassName = additionalReader.getClassName();
            reader.setFactory(FactoryUtils.factory(readerClassName, applicationResultBuildItem.getSingletonClasses(), recorder,
                    beanContainerBuildItem));
            reader.setConstraint(additionalReader.getRuntimeType());
            if (!additionalReader.getMediaTypeStrings().isEmpty()) {
                reader.setMediaTypeStrings(additionalReader.getMediaTypeStrings());
            }
            recorder.registerReader(serialisers, additionalReader.getHandledClassName(), reader);
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, false, readerClassName));
        }

    }
}
