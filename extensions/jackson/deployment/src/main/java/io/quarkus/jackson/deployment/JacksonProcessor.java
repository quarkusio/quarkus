package io.quarkus.jackson.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;

public class JacksonProcessor {

    @BuildStep
    void register(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false,
                "com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector",
                "com.fasterxml.jackson.databind.ser.std.SqlDateSerializer"));
    }

}
