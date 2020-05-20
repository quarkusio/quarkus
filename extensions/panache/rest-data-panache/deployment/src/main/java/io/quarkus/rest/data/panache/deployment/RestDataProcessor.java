package io.quarkus.rest.data.panache.deployment;

import java.util.Arrays;
import java.util.List;

import org.jboss.resteasy.links.impl.EL;

import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.jackson.spi.JacksonModuleBuildItem;
import io.quarkus.jsonb.spi.JsonbSerializerBuildItem;
import io.quarkus.rest.data.panache.runtime.hal.HalCollectionWrapper;
import io.quarkus.rest.data.panache.runtime.hal.HalCollectionWrapperJacksonSerializer;
import io.quarkus.rest.data.panache.runtime.hal.HalCollectionWrapperJsonbSerializer;
import io.quarkus.rest.data.panache.runtime.hal.HalEntityWrapper;
import io.quarkus.rest.data.panache.runtime.hal.HalEntityWrapperJacksonSerializer;
import io.quarkus.rest.data.panache.runtime.hal.HalEntityWrapperJsonbSerializer;
import io.quarkus.rest.data.panache.runtime.hal.HalLink;
import io.quarkus.rest.data.panache.runtime.hal.HalLinkJacksonSerializer;
import io.quarkus.rest.data.panache.runtime.hal.HalLinkJsonbSerializer;

public class RestDataProcessor {

    @BuildStep
    void implementResources(CombinedIndexBuildItem index, List<RestDataResourceBuildItem> resourceBuildItems,
            BuildProducer<GeneratedBeanBuildItem> implementationsProducer) {
        ClassOutput classOutput = new GeneratedBeanGizmoAdaptor(implementationsProducer);
        RestDataResourceImplementor implementor = new RestDataResourceImplementor(index.getIndex());
        for (RestDataResourceBuildItem resourceBuildItem : resourceBuildItems) {
            implementor.implement(classOutput, resourceBuildItem.getResourceInfo());
        }
    }

    @BuildStep
    JacksonModuleBuildItem registerJacksonSerializers() {
        return new JacksonModuleBuildItem.Builder("hal-wrappers")
                .addSerializer(HalEntityWrapperJacksonSerializer.class.getName(), HalEntityWrapper.class.getName())
                .addSerializer(HalCollectionWrapperJacksonSerializer.class.getName(), HalCollectionWrapper.class.getName())
                .addSerializer(HalLinkJacksonSerializer.class.getName(), HalLink.class.getName())
                .build();
    }

    @BuildStep
    JsonbSerializerBuildItem registerJsonbSerializers() {
        return new JsonbSerializerBuildItem(Arrays.asList(
                HalEntityWrapperJsonbSerializer.class.getName(),
                HalCollectionWrapperJsonbSerializer.class.getName(),
                HalLinkJsonbSerializer.class.getName()));
    }

    @BuildStep
    RuntimeInitializedClassBuildItem el() {
        return new RuntimeInitializedClassBuildItem(EL.class.getCanonicalName());
    }
}
