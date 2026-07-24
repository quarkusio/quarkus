package io.quarkus.elytron.security.common.runtime;

import java.security.Security;

import org.wildfly.security.password.WildFlyElytronPasswordProvider;

/**
 * Runtime security utilities for the Elytron security common extension.
 */
public class ElytronCommonRecorder {

    /**
     * Register the WildFly Elytron password provider if not already present.
     * <p>
     * We don't remove this, as there is no correct place where it can be removed —
     * continuous testing can be running alongside the dev mode app, but there is
     * only ever one provider.
     */
    public static void registerPasswordProvider() {
        WildFlyElytronPasswordProvider provider = new WildFlyElytronPasswordProvider();
        if (Security.getProvider(provider.getName()) == null) {
            Security.addProvider(provider);
        }
    }
}
