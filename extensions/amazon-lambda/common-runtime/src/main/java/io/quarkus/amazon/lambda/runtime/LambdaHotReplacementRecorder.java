package io.quarkus.amazon.lambda.runtime;

import org.jboss.logging.Logger;

import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.dev.spi.HotReplacementContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class LambdaHotReplacementRecorder {
    private static final Logger log = Logger.getLogger(LambdaHotReplacementRecorder.class);

    static volatile long nextUpdate;
    public static volatile boolean enabled;
    private static final long HOT_REPLACEMENT_INTERVAL = 2000;

    static Object syncLock = new Object();

    public static boolean checkHotReplacement() throws Exception {
        if (!enabled)
            return false;
        HotReplacementContext hotReplacementContext = DevConsoleManager.getHotReplacementContext();
        if (hotReplacementContext == null)
            return false;

        if ((nextUpdate > System.currentTimeMillis() && !hotReplacementContext.isTest())) {
            if (hotReplacementContext.getDeploymentProblem() != null) {
                throw new Exception("Hot Replacement Deployment issue", hotReplacementContext.getDeploymentProblem());
            }
            return false;
        }
        synchronized (syncLock) {
            if (nextUpdate < System.currentTimeMillis() || hotReplacementContext.isTest()) {
                nextUpdate = System.currentTimeMillis() + HOT_REPLACEMENT_INTERVAL;
                try {
                    boolean restart = hotReplacementContext.doScan(true);
                    return restart;
                } catch (Exception e) {
                    throw new IllegalStateException("Unable to perform live reload scanning", e);
                }
            }
        }
        return false;
    }

    public void enable() {
        enabled = true;
    }
}
