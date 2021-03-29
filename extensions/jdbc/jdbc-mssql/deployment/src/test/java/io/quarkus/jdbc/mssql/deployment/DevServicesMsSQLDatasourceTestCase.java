package io.quarkus.jdbc.mssql.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration;
import io.agroal.api.exceptionsorter.MSSQLExceptionSorter;
import io.quarkus.test.QuarkusUnitTest;

public class DevServicesMsSQLDatasourceTestCase {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
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
        assertTrue(configuration.connectionFactoryConfiguration().jdbcUrl().contains("jdbc:sqlserver:"));
        assertEquals("SA", configuration.connectionFactoryConfiguration().principal().getName());
        assertEquals(20, configuration.maxSize());
        assertThat(configuration.exceptionSorter()).isInstanceOf(MSSQLExceptionSorter.class);

        try (Connection connection = dataSource.getConnection()) {
        }
    }
}
