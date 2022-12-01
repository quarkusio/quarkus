package io.quarkus.jdbc.db2.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration;
import io.agroal.api.exceptionsorter.DB2ExceptionSorter;
import io.quarkus.test.QuarkusUnitTest;

public class DevServicesDB2DatasourceTestCase {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("container-license-acceptance.txt"))
            // Expect no warnings (in particular from Agroal)
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue()
                    // There are other warnings: JDK8, TestContainers, drivers, ...
                    // Ignore them: we're only interested in Agroal here.
                    && record.getMessage().contains("Agroal"))
            .assertLogRecords(records -> assertThat(records)
                    // This is just to get meaningful error messages, as LogRecord doesn't have a toString()
                    .extracting(LogRecord::getMessage)
                    .isEmpty());

    @Inject
    AgroalDataSource dataSource;

    @Test
    public void testDatasource() throws Exception {
        AgroalConnectionPoolConfiguration configuration = dataSource.getConfiguration().connectionPoolConfiguration();
        assertTrue(configuration.connectionFactoryConfiguration().jdbcUrl().contains("jdbc:db2:"));
        assertEquals("quarkus", configuration.connectionFactoryConfiguration().principal().getName());
        assertEquals(20, configuration.maxSize());
        assertThat(configuration.exceptionSorter()).isInstanceOf(DB2ExceptionSorter.class);

        try (Connection connection = dataSource.getConnection()) {
        }
    }
}
