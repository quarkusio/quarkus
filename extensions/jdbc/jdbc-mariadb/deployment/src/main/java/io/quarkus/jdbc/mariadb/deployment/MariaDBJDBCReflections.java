package io.quarkus.jdbc.mariadb.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

public final class MariaDBJDBCReflections {

    @BuildStep
    void build(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        //Not strictly necessary when using Agroal, as it also registers
        //any JDBC driver being configured explicitly through its configuration.
        //We register it for the sake of people not using Agroal.
        final String driverName = "org.mariadb.jdbc.Driver";
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, driverName));

        //MariaDB's connection process requires reflective read to all fields of Options:
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, "org.mariadb.jdbc.util.Options"));
    }

    @BuildStep
    void runtimeInit(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitialized) {
        //MastersSlavesListener starts threads in DynamicSizedSchedulerImpl which is disallowed during build time in GraalVM
        runtimeInitialized
                .produce(new RuntimeInitializedClassBuildItem("org.mariadb.jdbc.internal.failover.impl.MastersSlavesListener"));
    }
}
