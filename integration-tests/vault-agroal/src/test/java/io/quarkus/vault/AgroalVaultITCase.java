package io.quarkus.vault;

import static io.quarkus.vault.test.VaultTestExtension.testDataSource;

import java.sql.SQLException;

import javax.inject.Inject;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.vault.test.VaultTestLifecycleManager;

@DisabledOnOs(OS.WINDOWS) // https://github.com/quarkusio/quarkus/issues/3796
@QuarkusTestResource(VaultTestLifecycleManager.class)
public class AgroalVaultITCase {

    private static final Logger log = Logger.getLogger(AgroalVaultITCase.class.getName());

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("application-vault-datasource.properties", "application.properties"));

    @Inject
    @DataSource("staticDS")
    AgroalDataSource staticDS;

    @Inject
    @DataSource("dynamicDS")
    AgroalDataSource dynamicDS;

    @Test
    public void staticDS() throws SQLException {
        testDataSource(staticDS);
    }

    @Test
    public void dynamicDS() throws SQLException {
        testDataSource(dynamicDS);
    }

}
