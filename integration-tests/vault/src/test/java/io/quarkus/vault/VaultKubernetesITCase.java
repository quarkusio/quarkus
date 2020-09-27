package io.quarkus.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.vault.runtime.config.VaultAuthenticationType;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;
import io.quarkus.vault.test.VaultTestLifecycleManager;

@DisabledOnOs(OS.WINDOWS) // https://github.com/quarkusio/quarkus/issues/3796
@QuarkusTestResource(VaultTestLifecycleManager.class)
public class VaultKubernetesITCase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("application-vault-kubernetes.properties", "application.properties"));

    @Inject
    VaultRuntimeConfig runtimeConfig;

    @Test
    public void testGetAuthenticationType() {
        assertEquals(VaultAuthenticationType.KUBERNETES, runtimeConfig.getAuthenticationType());
    }

    @Test
    public void testRole() {
        assertEquals(Optional.of("test"), runtimeConfig.authentication.kubernetes.role);
    }

    @Test
    public void testAuthMountPath() {
        assertEquals("auth/test", runtimeConfig.authentication.kubernetes.authMountPath);
    }

}
