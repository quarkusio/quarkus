package io.quarkus.agroal.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.sql.Connection;
import java.sql.SQLException;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration;
import io.agroal.api.transaction.TransactionIntegration;
import io.agroal.narayana.NarayanaTransactionIntegration;
import io.quarkus.test.QuarkusUnitTest;

public class DisabledTransactionDataSourceConfigTest {

    @Inject
    AgroalDataSource defaultDataSource;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("base.properties")
            .overrideConfigKey("quarkus.datasource.jdbc.transactions", "DISABLED")
            .overrideConfigKey("quarkus.datasource.jdbc.detect-statement-leaks", "false");

    @Test
    public void testNonTransactionalDataSourceInjection() throws SQLException {
        AgroalConnectionPoolConfiguration configuration = defaultDataSource.getConfiguration().connectionPoolConfiguration();

        assertFalse(configuration.transactionIntegration() instanceof NarayanaTransactionIntegration);
        Class<? extends TransactionIntegration> nonTxIntegrator = TransactionIntegration.none().getClass();
        assertEquals(nonTxIntegrator, configuration.transactionIntegration().getClass());
        assertFalse(configuration.connectionFactoryConfiguration().trackJdbcResources());

        try (Connection connection = defaultDataSource.getConnection()) {
        }
    }
}
