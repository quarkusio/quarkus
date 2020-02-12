package io.quarkus.elytron.security.common.runtime;

import java.security.Security;

import org.jboss.logging.Logger;
import org.wildfly.security.password.WildFlyElytronPasswordProvider;

import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

/**
 * The runtime security recorder class that provides methods for creating RuntimeValues for the deployment security objects.
 */
@Recorder
public class ElytronCommonRecorder {
    static final Logger log = Logger.getLogger(ElytronCommonRecorder.class);

    /**
     * As of Graal 19.3.0 this has to be registered at runtime, due to a bug.
     *
     * 19.3.1 should fix this, see https://github.com/oracle/graal/issues/1883
     */
    public void registerPasswordProvider(ShutdownContext shutdownContext) {
        WildFlyElytronPasswordProvider provider = new WildFlyElytronPasswordProvider();
        Security.addProvider(provider);
        shutdownContext.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                Security.removeProvider(provider.getName());
            }
        });
    }
}
