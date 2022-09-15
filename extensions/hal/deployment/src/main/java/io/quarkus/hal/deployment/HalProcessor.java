package io.quarkus.hal.deployment;

import java.util.Arrays;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.hal.HalCollectionWrapper;
import io.quarkus.hal.HalCollectionWrapperJacksonSerializer;
import io.quarkus.hal.HalCollectionWrapperJsonbSerializer;
import io.quarkus.hal.HalEntityWrapper;
import io.quarkus.hal.HalEntityWrapperJacksonSerializer;
import io.quarkus.hal.HalEntityWrapperJsonbSerializer;
import io.quarkus.hal.HalLink;
import io.quarkus.hal.HalLinkJacksonSerializer;
import io.quarkus.hal.HalLinkJsonbSerializer;
import io.quarkus.jackson.spi.JacksonModuleBuildItem;
import io.quarkus.jsonb.spi.JsonbSerializerBuildItem;

public class HalProcessor {

    @BuildStep
    ReflectiveClassBuildItem registerReflection() {
        return new ReflectiveClassBuildItem(true, true, HalLink.class);
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

}
