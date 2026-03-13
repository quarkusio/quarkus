package io.quarkus.aesh.runtime.devmode;

import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.dev.spi.HotReplacementContext;
import io.quarkus.dev.spi.HotReplacementSetup;

/**
 * Dev mode hot replacement integration for aesh remote transports (SSH, WebSocket).
 * <p>
 * Follows the same pattern as the gRPC extension: scans for source changes
 * on each new connection, rate-limited to at most once every two seconds.
 * If changes are detected, the application restarts and the client reconnects
 * with updated code.
 */
public class AeshHotReplacementSetup implements HotReplacementSetup {

    private static final Logger LOG = Logger.getLogger(AeshHotReplacementSetup.class);
    private static final long TWO_SECONDS = 2000;

    private HotReplacementContext context;
    private volatile long nextUpdate;

    @Override
    public void setupHotDeployment(HotReplacementContext context) {
        this.context = context;
        context.addPreRestartStep(DevModeConnection::notifyAllReloading);
        AeshHotReplacementInterceptor.register(new ScanHandler());
    }

    private class ScanHandler implements Supplier<Boolean> {
        @Override
        public Boolean get() {
            boolean restarted = false;
            synchronized (this) {
                if (nextUpdate < System.currentTimeMillis() || context.isTest()) {
                    try {
                        restarted = context.doScan(true);
                        if (context.getDeploymentProblem() != null) {
                            LOG.error("Failed to redeploy application on changes",
                                    context.getDeploymentProblem());
                        }
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    nextUpdate = System.currentTimeMillis() + TWO_SECONDS;
                }
            }
            return restarted;
        }
    }
}
