package io.quarkus.jaxrs.client.deployment;

import java.util.List;

import javax.ws.rs.RuntimeType;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.jaxrs.client.runtime.JaxrsClientRecorder;
import io.quarkus.rest.common.deployment.ApplicationResultBuildItem;
import io.quarkus.rest.common.deployment.SerializersUtil;
import io.quarkus.rest.common.runtime.core.Serialisers;
import io.quarkus.rest.spi.ClientProxiesBuildItem;
import io.quarkus.rest.spi.MessageBodyReaderBuildItem;
import io.quarkus.rest.spi.MessageBodyWriterBuildItem;

public class JaxrsClientProcessor {

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void setupClientProxies(ClientProxiesBuildItem clientProxiesBuildItem, JaxrsClientRecorder recorder,
            BeanContainerBuildItem beanContainerBuildItem,
            ApplicationResultBuildItem applicationResultBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItemBuildProducer,
            List<MessageBodyReaderBuildItem> messageBodyReaderBuildItems,
            List<MessageBodyWriterBuildItem> messageBodyWriterBuildItems) {
        if (clientProxiesBuildItem != null) {
            recorder.setupClientProxies(clientProxiesBuildItem.getClientProxies());
        }
        Serialisers serialisers = recorder.createSerializers();

        SerializersUtil.setupSerializers(recorder, reflectiveClassBuildItemBuildProducer, messageBodyReaderBuildItems,
                messageBodyWriterBuildItems, beanContainerBuildItem, applicationResultBuildItem, serialisers,
                RuntimeType.CLIENT);

    }

}
