package io.quarkus.panache.rest.common.deployment;

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
import io.quarkus.panache.rest.common.runtime.hal.HalCollectionWrapper;
import io.quarkus.panache.rest.common.runtime.hal.HalCollectionWrapperJacksonSerializer;
import io.quarkus.panache.rest.common.runtime.hal.HalCollectionWrapperJsonbSerializer;
import io.quarkus.panache.rest.common.runtime.hal.HalEntityWrapper;
import io.quarkus.panache.rest.common.runtime.hal.HalEntityWrapperJacksonSerializer;
import io.quarkus.panache.rest.common.runtime.hal.HalEntityWrapperJsonbSerializer;
import io.quarkus.panache.rest.common.runtime.hal.HalLink;
import io.quarkus.panache.rest.common.runtime.hal.HalLinkJacksonSerializer;
import io.quarkus.panache.rest.common.runtime.hal.HalLinkJsonbSerializer;

public class PanacheRestProcessor {

    @BuildStep
    void implementCrudResources(CombinedIndexBuildItem index, List<PanacheCrudResourceInfo> resources,
            BuildProducer<GeneratedBeanBuildItem> implementationsProducer) {
        ClassOutput classOutput = new GeneratedBeanGizmoAdaptor(implementationsProducer);
        CrudResourceImplementor implementor = new CrudResourceImplementor(index.getIndex());
        for (PanacheCrudResourceInfo resource : resources) {
            implementor.implement(classOutput, resource);
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
