package io.quarkus.resteasy.reactive.common.deployment;

import java.util.List;

import javax.ws.rs.RuntimeType;

import org.jboss.resteasy.reactive.common.core.Serialisers;
import org.jboss.resteasy.reactive.common.model.ResourceReader;
import org.jboss.resteasy.reactive.common.model.ResourceWriter;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.resteasy.reactive.common.runtime.QuarkusRestCommonRecorder;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;
import io.quarkus.resteasy.reactive.spi.RuntimeTypeItem;

public class SerializersUtil {

    public static void setupSerializers(QuarkusRestCommonRecorder recorder,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            List<MessageBodyReaderBuildItem> messageBodyReaderBuildItems,
            List<MessageBodyWriterBuildItem> messageBodyWriterBuildItems,
            BeanContainerBuildItem beanContainerBuildItem,
            ApplicationResultBuildItem applicationResultBuildItem,
            Serialisers serialisers, RuntimeType runtimeType) {

        for (MessageBodyWriterBuildItem additionalWriter : RuntimeTypeItem.filter(messageBodyWriterBuildItems,
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
        for (MessageBodyReaderBuildItem additionalReader : RuntimeTypeItem.filter(messageBodyReaderBuildItems,
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
