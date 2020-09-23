package io.quarkus.vault;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.vault.sys.VaultSealStatus;
import io.quarkus.vault.test.VaultTestLifecycleManager;

@DisabledOnOs(OS.WINDOWS) // https://github.com/quarkusio/quarkus/issues/3796
@QuarkusTestResource(VaultTestLifecycleManager.class)
public class VaultSysITCase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("application-vault.properties", "application.properties"));

    @Inject
    VaultSystemBackendEngine vaultSystemBackendEngine;

    @Test
    public void testSealStatus() {
        final VaultSealStatus vaultSealStatus = vaultSystemBackendEngine.sealStatus();
        assertThat(vaultSealStatus).returns("shamir", VaultSealStatus::getType);
        assertThat(vaultSealStatus).returns(false, VaultSealStatus::isSealed);
    }

    @Test
    public void policy() {
        String rules = "path \"transit/*\" {\n" +
                "  capabilities = [ \"create\", \"read\", \"update\" ]\n" +
                "}";
        String name = "sys-test-policy";
        vaultSystemBackendEngine.createUpdatePolicy(name, rules);
        List<String> policies = vaultSystemBackendEngine.getPolicies();
        assertTrue(policies.contains(name));
        String policyRules = vaultSystemBackendEngine.getPolicyRules(name);
        assertEquals(rules, policyRules);
        vaultSystemBackendEngine.deletePolicy(name);
        policies = vaultSystemBackendEngine.getPolicies();
        assertFalse(policies.contains(name));
    }
}
