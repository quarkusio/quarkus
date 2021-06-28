package io.quarkus.elytron.security.common.runtime;

import java.security.Security;

import org.wildfly.security.password.WildFlyElytronPasswordProvider;

import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

/**
 * The runtime security recorder class that provides methods for creating RuntimeValues for the deployment security objects.
 */
@Recorder
public class ElytronCommonRecorder {

    public void registerPasswordProvider(ShutdownContext shutdownContext) {
        WildFlyElytronPasswordProvider provider = new WildFlyElytronPasswordProvider();
        if (Security.getProvider(provider.getName()) == null) {
            Security.addProvider(provider);
            shutdownContext.addShutdownTask(new Runnable() {
                @Override
                public void run() {
                    Security.removeProvider(provider.getName());
                }
            });
        }
    }
}
