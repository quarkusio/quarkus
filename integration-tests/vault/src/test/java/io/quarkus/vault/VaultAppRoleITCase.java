package io.quarkus.vault;

import static io.quarkus.vault.test.VaultTestExtension.APP_SECRET_PATH;
import static io.quarkus.vault.test.VaultTestExtension.SECRET_KEY;
import static io.quarkus.vault.test.VaultTestExtension.SECRET_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.vault.test.VaultTestLifecycleManager;

@DisabledOnOs(OS.WINDOWS) // https://github.com/quarkusio/quarkus/issues/3796
@QuarkusTestResource(VaultTestLifecycleManager.class)
public class VaultAppRoleITCase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-vault-approle.properties", "application.properties"));

    @Inject
    VaultKVSecretEngine kvSecretEngine;

    @Test
    public void secretV2() {
        Map<String, String> secrets = kvSecretEngine.readSecret(APP_SECRET_PATH);
        assertEquals("{" + SECRET_KEY + "=" + SECRET_VALUE + "}", secrets.toString());
    }

}
