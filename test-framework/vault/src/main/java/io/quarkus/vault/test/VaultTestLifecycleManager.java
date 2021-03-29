package io.quarkus.vault.test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class VaultTestLifecycleManager implements QuarkusTestResourceLifecycleManager {

    private static final Logger log = Logger.getLogger(VaultTestLifecycleManager.class);

    protected VaultTestExtension vaultTestExtension;

    public static final String GRAALVM_JRE_LIB_AMD_64 = "/opt/graalvm/jre/lib/amd64";

    @Override
    public Map<String, String> start() {

        vaultTestExtension = new VaultTestExtension();

        Map<String, String> sysprops = new HashMap<>();

        // see TLS availability in native mode https://github.com/quarkusio/quarkus/issues/3797
        if (VaultTestExtension.useTls()) {
            sysprops.put("javax.net.ssl.trustStore", "src/test/resources/vaultTrustStore");
            sysprops.put("java.library.path", GRAALVM_JRE_LIB_AMD_64);
        }

        try {
            vaultTestExtension.start();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }

        sysprops.put("vault-test.role-id", vaultTestExtension.appRoleRoleId);
        sysprops.put("vault-test.secret-id", vaultTestExtension.appRoleSecretId);

        sysprops.put("vault-test.secret-id-wrapping-token", vaultTestExtension.appRoleSecretIdWrappingToken);
        sysprops.put("vault-test.client-token-wrapping-token", vaultTestExtension.clientTokenWrappingToken);
        sysprops.put("vault-test.password-kv-v1-wrapping-token", vaultTestExtension.passwordKvv1WrappingToken);
        sysprops.put("vault-test.password-kv-v2-wrapping-token", vaultTestExtension.passwordKvv2WrappingToken);
        sysprops.put("vault-test.another-password-kv-v2-wrapping-token", vaultTestExtension.anotherPasswordKvv2WrappingToken);

        log.info("using system properties " + sysprops);

        return sysprops;
    }

    @Override
    public void stop() {
        if (vaultTestExtension != null) {
            vaultTestExtension.close();
        }
    }
}
