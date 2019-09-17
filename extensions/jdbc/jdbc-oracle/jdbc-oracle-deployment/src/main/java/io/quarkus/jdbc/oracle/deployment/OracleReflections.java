package io.quarkus.jdbc.oracle.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;

/**
 * Registers the {@code oracle.jdbc.driver.OracleDriver} so that it can be loaded
 * by reflection, as commonly expected.
 *
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
public final class OracleReflections {

    @BuildStep
    void build(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        //Not strictly necessary when using Agroal, as it also registers
        //any JDBC driver being configured explicitly through its configuration.
        //We register it for the sake of people not using Agroal.
        final String driverName = "oracle.jdbc.driver.OracleDriver";
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, driverName));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true,
                "oracle.jdbc.driver.T4CDriverExtension",
                "oracle.sql.AnyDataFactory",
                "oracle.sql.TypeDescriptorFactory"));
    }

}
