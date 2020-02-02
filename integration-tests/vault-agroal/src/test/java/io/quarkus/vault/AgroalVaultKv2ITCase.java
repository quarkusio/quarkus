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
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.vault.test.VaultTestLifecycleManager;

@DisabledOnOs(OS.WINDOWS) // https://github.com/quarkusio/quarkus/issues/3796
@QuarkusTestResource(VaultTestLifecycleManager.class)
public class AgroalVaultKv2ITCase {

    private static final Logger log = Logger.getLogger(AgroalVaultKv2ITCase.class.getName());

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("application-vault-kv-version2-datasource.properties", "application.properties"));

    @Inject
    AgroalDataSource staticDataSourceV2;

    @Test
    public void staticKvVersion2() throws SQLException {
        testDataSource(staticDataSourceV2);
    }

}
