package io.quarkus.jdbc.db2.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that Dev Services for DB2 correctly handles datasource names
 * longer than 8 characters (DB2's database name limit).
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/51225">GitHub Issue #51225</a>
 */
public class DevServicesDB2LongDatasourceNameTestCase {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withConfigurationResource("long-datasource-name.properties")
            // Verify that a warning is logged about truncating the database name
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue()
                    && record.getMessage().contains("8 character limit"))
            .assertLogRecords(records -> assertThat(records)
                    .extracting(LogRecord::getMessage)
                    // warnf() returns format string with %s, not the formatted message
                    .anyMatch(msg -> msg.contains("8 character limit")));

    @Inject
    @DataSource("additional")
    AgroalDataSource dataSource;

    @Test
    public void testLongDatasourceNameIsTruncated() throws Exception {
        // If we get here without an error, the database name was successfully truncated
        // and DB2 accepted the connection
        assertThat(dataSource).isNotNull();
        assertTrue(dataSource.getConfiguration().connectionPoolConfiguration()
                .connectionFactoryConfiguration().jdbcUrl().contains("jdbc:db2:"));

        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.isValid(5)).isTrue();
        }
    }
}
