package io.quarkus.jdbc.mariadb.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;

public final class MariaDBJDBCReflections {

    @BuildStep
    void build(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        //Not strictly necessary when using Agroal, as it also registers
        //any JDBC driver being configured explicitly through its configuration.
        //We register it for the sake of people not using Agroal.
        final String driverName = "org.mariadb.jdbc.Driver";
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, driverName));

        //MariaDB's connection process requires reflective read to all fields of Options:
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, "org.mariadb.jdbc.internal.util.Options"));
    }
}
