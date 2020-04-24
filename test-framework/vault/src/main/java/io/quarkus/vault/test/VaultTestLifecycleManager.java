package io.quarkus.vault.test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class VaultTestLifecycleManager implements QuarkusTestResourceLifecycleManager {

    private static final Logger log = Logger.getLogger(VaultTestLifecycleManager.class);

    private VaultTestExtension vaultTestExtension = new VaultTestExtension();

    public static final String GRAALVM_JRE_LIB_AMD_64 = "/opt/graalvm/jre/lib/amd64";

    @Override
    public Map<String, String> start() {

        Map<String, String> testProperties = new HashMap<>();

        // see TLS availability in native mode https://github.com/quarkusio/quarkus/issues/3797
        if (VaultTestExtension.useTls()) {
            testProperties.put("javax.net.ssl.trustStore", "src/test/resources/vaultTrustStore");
            testProperties.put("java.library.path", GRAALVM_JRE_LIB_AMD_64);
        }

        try {
            vaultTestExtension.start();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }

        testProperties.put("quarkus.vault.url", VaultTestExtension.getMappedVaultUrl());

        testProperties.put("vault-test.role-id", vaultTestExtension.appRoleRoleId);
        testProperties.put("vault-test.secret-id", vaultTestExtension.appRoleSecretId);

        testProperties.put("vault-test.secret-id-wrapping-token", vaultTestExtension.appRoleSecretIdWrappingToken);
        testProperties.put("vault-test.client-token-wrapping-token", vaultTestExtension.clientTokenWrappingToken);
        testProperties.put("vault-test.password-kv-v1-wrapping-token", vaultTestExtension.passwordKvv1WrappingToken);
        testProperties.put("vault-test.password-kv-v2-wrapping-token", vaultTestExtension.passwordKvv2WrappingToken);
        testProperties.put("vault-test.another-password-kv-v2-wrapping-token",
                vaultTestExtension.anotherPasswordKvv2WrappingToken);

        testProperties.put("vault-postgres-url",
                "jdbc:postgresql://localhost:" + VaultTestExtension.getMappedPostgresqlPort() + "/mydb");
        log.info("using system properties " + testProperties);

        return testProperties;
    }

    @Override
    public void stop() {
        vaultTestExtension.close();
    }
}
