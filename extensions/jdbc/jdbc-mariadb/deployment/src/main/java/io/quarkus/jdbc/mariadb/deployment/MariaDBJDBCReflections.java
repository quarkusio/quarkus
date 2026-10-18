package io.quarkus.jdbc.mariadb.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public final class MariaDBJDBCReflections {

    @BuildStep
    void build(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        //Not strictly necessary when using Agroal, as it also registers
        //any JDBC driver being configured explicitly through its configuration.
        //We register it for the sake of people not using Agroal.
        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder("org.mariadb.jdbc.Driver").build());

        //MariaDB's connection process requires reflective access to both fields and methods of Configuration and its Builder:
        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder("org.mariadb.jdbc.Configuration", "org.mariadb.jdbc.Configuration$Builder")
                        .fields().methods().build());
    }
}
