package io.quarkus.rest.data.panache.deployment;

import java.util.Arrays;
import java.util.List;

import org.jboss.resteasy.links.impl.EL;

import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.jackson.spi.JacksonModuleBuildItem;
import io.quarkus.jsonb.spi.JsonbSerializerBuildItem;
import io.quarkus.rest.data.panache.deployment.properties.ResourceProperties;
import io.quarkus.rest.data.panache.deployment.properties.ResourcePropertiesBuildItem;
import io.quarkus.rest.data.panache.deployment.properties.ResourcePropertiesProvider;
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
    ReflectiveClassBuildItem registerReflection() {
        return new ReflectiveClassBuildItem(true, true, HalLink.class);
    }

    @BuildStep
    void implementResources(CombinedIndexBuildItem index, List<RestDataResourceBuildItem> resourceBuildItems,
            List<ResourcePropertiesBuildItem> resourcePropertiesBuildItems, Capabilities capabilities,
            BuildProducer<GeneratedBeanBuildItem> implementationsProducer) {
        ClassOutput classOutput = new GeneratedBeanGizmoAdaptor(implementationsProducer);
        JaxRsResourceImplementor jaxRsResourceImplementor = new JaxRsResourceImplementor(hasValidatorCapability(capabilities));
        ResourcePropertiesProvider resourcePropertiesProvider = new ResourcePropertiesProvider(index.getIndex());

        for (RestDataResourceBuildItem resourceBuildItem : resourceBuildItems) {
            ResourceMetadata resourceMetadata = resourceBuildItem.getResourceMetadata();
            ResourceProperties resourceProperties = getResourceProperties(resourcePropertiesProvider,
                    resourceMetadata, resourcePropertiesBuildItems);
            if (resourceProperties.isHal() && !hasHalCapability(capabilities)) {
                throw new IllegalStateException(
                        "Cannot generate HAL endpoints without a RESTEasy JSON-B or Jackson capability");
            }
            if (resourceProperties.isExposed()) {
                jaxRsResourceImplementor.implement(classOutput, resourceMetadata, resourceProperties);
            }
        }
    }

    @BuildStep
    JacksonModuleBuildItem registerJacksonSerializers() {
        return new JacksonModuleBuildItem.Builder("hal-wrappers")
                .addSerializer(HalEntityWrapperJacksonSerializer.class.getName(), HalEntityWrapper.class.getName())
                .addSerializer(HalCollectionWrapperJacksonSerializer.class.getName(),
                        HalCollectionWrapper.class.getName())
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

    private ResourceProperties getResourceProperties(ResourcePropertiesProvider resourcePropertiesProvider,
            ResourceMetadata resourceMetadata, List<ResourcePropertiesBuildItem> resourcePropertiesBuildItems) {
        for (ResourcePropertiesBuildItem resourcePropertiesBuildItem : resourcePropertiesBuildItems) {
            if (resourcePropertiesBuildItem.getResourceType().equals(resourceMetadata.getResourceClass())
                    || resourcePropertiesBuildItem.getResourceType().equals(resourceMetadata.getResourceInterface())) {
                return resourcePropertiesBuildItem.getResourcePropertiesInfo();
            }
        }
        return resourcePropertiesProvider.getForInterface(resourceMetadata.getResourceInterface());
    }

    private boolean hasValidatorCapability(Capabilities capabilities) {
        return capabilities.isPresent(Capability.HIBERNATE_VALIDATOR);
    }

    private boolean hasHalCapability(Capabilities capabilities) {
        return capabilities.isPresent(Capability.RESTEASY_JSON_JSONB)
                || capabilities.isPresent(Capability.RESTEASY_JSON_JACKSON);
    }
}
