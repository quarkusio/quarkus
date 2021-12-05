package io.quarkus.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.vault.test.VaultTestLifecycleManager;

@DisabledOnOs(OS.WINDOWS) // https://github.com/quarkusio/quarkus/issues/3796
@QuarkusTestResource(VaultTestLifecycleManager.class)
public class VaultKubernetesITCase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-vault-kubernetes.properties", "application.properties"));

    @ConfigProperty(name = "quarkus.vault.authentication.kubernetes.role")
    String role;

    @ConfigProperty(name = "quarkus.vault.authentication.kubernetes.auth-mount-path")
    String path;

    @Test
    public void testRole() {
        assertEquals("test", role);
    }

    @Test
    public void testAuthMountPath() {
        assertEquals("auth/test", path);
    }

}
