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

        Map<String, String> sysprops = new HashMap<>();

        // see TLS availability in native mode https://github.com/quarkusio/quarkus/issues/3797
        if (VaultTestExtension.useTls()) {
            sysprops.put("quarkus.vault.url", "https://localhost:8200");
            sysprops.put("javax.net.ssl.trustStore", "src/test/resources/vaultTrustStore");
            sysprops.put("java.library.path", GRAALVM_JRE_LIB_AMD_64);
        }

        try {
            vaultTestExtension.start();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }

        sysprops.put("quarkus.vault.authentication.app-role.role-id", vaultTestExtension.appRoleRoleId);
        sysprops.put("quarkus.vault.authentication.app-role.secret-id", vaultTestExtension.appRoleSecretId);

        log.info("using system properties " + sysprops);

        return sysprops;
    }

    @Override
    public void stop() {
        vaultTestExtension.close();
    }
}
