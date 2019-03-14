package io.quarkus.jdbc.oracle.deployment;

import static io.quarkus.deployment.Feature.JDBC_ORACLE;

import io.quarkus.agroal.spi.JdbcDriverBuildItem;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

/**
 * N.B. this processor is relatively simple as we rely on the /META-INF/native-image/
 * resources provided by the driver.
 * This should probably change, as we could probably generate better optimised
 * code by bypassing the static definitions coming with the driver.
 */
public class OracleProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(JDBC_ORACLE);
    }

    @BuildStep
    void registerDriver(BuildProducer<JdbcDriverBuildItem> jdbcDriver) {
        jdbcDriver.produce(new JdbcDriverBuildItem(DatabaseKind.ORACLE, "oracle.jdbc.driver.OracleDriver",
                "oracle.jdbc.xa.client.OracleXADataSource"));
    }

}
