package io.quarkus.agroal.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration;
import io.agroal.narayana.NarayanaTransactionIntegration;
import io.quarkus.agroal.DataSource;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.QuarkusUnitTest;

public class XaDataSourceConfigTest {

    //tag::injection[]
    @Inject
    @DataSource("xa")
    AgroalDataSource xaRecoverDS;

    @Inject
    @DataSource("xaNoRecover")
    AgroalDataSource xaNoRecoverDS;
    //end::injection[]

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-recovery-datasources.properties");

    @Test
    @ActivateRequestContext
    public void testEnlistDatasourcesWithRecovery() throws SQLException {
        AgroalConnectionPoolConfiguration xaRecoverConfig = xaRecoverDS.getConfiguration().connectionPoolConfiguration();
        AgroalConnectionPoolConfiguration xaNoRecoverConfig = xaNoRecoverDS.getConfiguration().connectionPoolConfiguration();

        assertTrue(xaRecoverConfig.recoveryEnable(), "xaRecoverDS datasource should have recover enabled");
        assertFalse(xaNoRecoverConfig.recoveryEnable(), "xaNoRecoverDS datasource should not have recover enabled");

        assertInstanceOf(NarayanaTransactionIntegration.class, xaRecoverConfig.transactionIntegration(),
                "Agroal transaction integration should use Narayana for xaRecoverDS");
        assertInstanceOf(NarayanaTransactionIntegration.class, xaNoRecoverConfig.transactionIntegration(),
                "Agroal transaction integration should use Narayana for xaNoRecoverDS");

        // run a transaction and use the two datasources, ensuring that it commits ok
        QuarkusTransaction.begin();

        // Remark: the two datasources will have been registered with the transaction recovery system because the config
        // includes quarkus.transaction-manager.enable-recovery=true
        // see QuarkusRecoveryService for details of how the recovery service manages connections to datasources
        try (var conn = xaRecoverDS.getConnection()) {
            assertFalse(conn.getAutoCommit(), "XA connection should not have the auto commit flag set");
            try (var conn2 = xaNoRecoverDS.getConnection()) {
                assertFalse(conn2.getAutoCommit(), "XA connection should not have the auto commit flag set");
            }
        }

        assertTrue(QuarkusTransaction.isActive(), "transaction should still have been active");

        QuarkusTransaction.commit();
    }
}
