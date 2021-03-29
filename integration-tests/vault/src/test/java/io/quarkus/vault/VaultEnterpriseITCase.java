package io.quarkus.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;

@DisabledOnOs(OS.WINDOWS) // https://github.com/quarkusio/quarkus/issues/3796
@QuarkusTestResource(WiremockVault.class)
public class VaultEnterpriseITCase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("application-vault-enterprise.properties", "application.properties"));

    @Inject
    VaultKVSecretEngine kvSecretEngine;

    @Test
    public void header() {
        assertEquals("{hello=world}", kvSecretEngine.readSecret("foo").toString());
    }
}
