package io.quarkus.vault;

import static io.quarkus.vault.test.VaultTestExtension.APP_SECRET_PATH;
import static io.quarkus.vault.test.VaultTestExtension.SECRET_KEY;
import static io.quarkus.vault.test.VaultTestExtension.SECRET_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import javax.inject.Inject;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.vault.test.VaultTestLifecycleManager;

@DisabledOnOs(OS.WINDOWS)
@QuarkusTestResource(VaultTestLifecycleManager.class)
public class VaultAppRoleITCase {

    private static final Logger log = Logger.getLogger(VaultITCase.class);

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("application-vault-approle.properties", "application.properties"));

    @Inject
    VaultKVSecretEngine kvSecretEngine;

    @Test
    public void secretV1() {
        Map<String, String> secrets = kvSecretEngine.readSecret(APP_SECRET_PATH);
        assertEquals("{" + SECRET_KEY + "=" + SECRET_VALUE + "}", secrets.toString());
    }

}
