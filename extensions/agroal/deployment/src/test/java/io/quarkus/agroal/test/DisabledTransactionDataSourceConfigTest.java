package io.quarkus.agroal.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
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
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("application-disabledjta-datasource.properties",
                            "application.properties"));

    @Test
    public void testNonTransactionalDataSourceInjection() throws SQLException {
        AgroalConnectionPoolConfiguration configuration = defaultDataSource.getConfiguration().connectionPoolConfiguration();

        assertTrue(!(configuration.transactionIntegration() instanceof NarayanaTransactionIntegration));
        Class<? extends TransactionIntegration> nonTxIntegrator = TransactionIntegration.none().getClass();
        assertEquals(nonTxIntegrator, configuration.transactionIntegration().getClass());

        try (Connection connection = defaultDataSource.getConnection()) {
        }
    }
}
