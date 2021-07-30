package io.quarkus.elytron.security.common.runtime;

import java.security.Security;

import org.wildfly.security.password.WildFlyElytronPasswordProvider;

import io.quarkus.runtime.annotations.Recorder;

/**
 * The runtime security recorder class that provides methods for creating RuntimeValues for the deployment security objects.
 */
@Recorder
public class ElytronCommonRecorder {

    public void registerPasswordProvider() {
        //we don't remove this, as there is no correct place where it can be removed
        //as continuous testing can be running along side the dev mode app, but there is
        //only ever one provider
        WildFlyElytronPasswordProvider provider = new WildFlyElytronPasswordProvider();
        if (Security.getProvider(provider.getName()) == null) {
            Security.addProvider(provider);
        }
    }
}
