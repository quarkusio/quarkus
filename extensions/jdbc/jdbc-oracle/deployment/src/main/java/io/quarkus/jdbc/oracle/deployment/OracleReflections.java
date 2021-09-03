package io.quarkus.jdbc.oracle.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

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
        // "oracle.jdbc.OracleDriver" is what's listed in the serviceloader resource from Oracle,
        // but it delegates all use to "oracle.jdbc.driver.OracleDriver" - which is also what's recommended by the docs.
        final String driverName = "oracle.jdbc.driver.OracleDriver";
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, driverName));
    }

}
