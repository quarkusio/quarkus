package io.quarkus.jdbc.postgresql.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

/**
 * Registers the {@code org.postgresql.Driver} so that it can be loaded
 * by reflection, as commonly expected.
 *
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
public final class PostgreSQLJDBCReflections {

    @BuildStep
    void build(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        //Not strictly necessary when using Agroal, as it also registers
        //any JDBC driver being configured explicitly through its configuration.
        //We register it for the sake of other users.
        final String driverName = "org.postgresql.Driver";
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, driverName));

        // Needed when quarkus.datasource.jdbc.transactions=xa for the setting of the username and password
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, true, false, "org.postgresql.ds.common.BaseDataSource"));
    }

}
