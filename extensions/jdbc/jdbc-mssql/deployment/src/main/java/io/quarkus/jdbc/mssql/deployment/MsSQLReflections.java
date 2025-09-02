package io.quarkus.jdbc.mssql.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

/**
 * Registers the {@code com.microsoft.sqlserver.jdbc.SQLServerDriver} so that it can be loaded
 * by reflection, as commonly expected.
 *
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
public final class MsSQLReflections {

    @BuildStep
    void build(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        //Not strictly necessary when using Agroal, as it also registers
        //any JDBC driver being configured explicitly through its configuration.
        //We register it for the sake of people not using Agroal.
        final String driverName = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(driverName).build());

        // https://github.com/quarkusio/quarkus/pull/48082#issuecomment-2912550391
        String className = "com.microsoft.sqlserver.jdbc.ConfigurableRetryLogic";
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(className).build());
    }

}
