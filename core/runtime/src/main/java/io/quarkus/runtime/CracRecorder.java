package io.quarkus.runtime;

import org.crac.Context;
import org.crac.Resource;
import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.Recorder;

/**
 * Registers a CRAC resource. Must be called in static initialization phase!
 */
@Recorder
public class CracRecorder {

    protected static class CracResource implements Resource {
        protected final boolean fullWarmup;

        public CracResource(boolean fullWarmup) {
            this.fullWarmup = fullWarmup;
        }

        @Override
        public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {
            if (fullWarmup) {
                Quarkus.manualStart();
            }
        }

        @Override
        public void afterRestore(Context<? extends Resource> context) throws Exception {
            if (!fullWarmup) {
                Quarkus.manualStart();
                Logger.getLogger(Quarkus.class).info("Started Quarkus via CRAC afterRestore()");
            } else {
                Logger.getLogger(Quarkus.class).info("Started Quarkus via CRAC beforeCheckpoint()");
            }
        }
    }

    public static boolean enabled = false;
    public static boolean fullWarmup = false;

    public void register(boolean fw) {
        enabled = true;
        fullWarmup = fw;
        // I originally wanted to do registration here, but for some reason
        // the registration didn't result in the CRAC callbacks being executed.
        // I had to move the registration into Quarkus.manualInitialize()
        //              Core.getGlobalContext()
        //                        .register(new CracResource(CracRecorder.fullWarmup));
    }

}
