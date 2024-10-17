package io.quarkus.jdbc.singlestore.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public final class SinglestoreJDBCReflections {

    @BuildStep
    void build(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        //Not strictly necessary when using Agroal, as it also registers
        //any JDBC driver being configured explicitly through its configuration.
        //We register it for the sake of people not using Agroal.
        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder("com.singlestore.jdbc.Driver").build());

        //Singlestore's connection process requires reflective read to all fields of Configuration and its Builder:
        reflectiveClass.produce(
                ReflectiveClassBuildItem
                        .builder("com.singlestore.jdbc.Configuration", "com.singlestore.jdbc.Configuration$Builder")
                        .fields().build());
    }
}
