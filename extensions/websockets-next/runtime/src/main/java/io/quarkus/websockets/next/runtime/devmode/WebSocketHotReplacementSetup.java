package io.quarkus.websockets.next.runtime.devmode;

import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.dev.spi.HotReplacementContext;
import io.quarkus.dev.spi.HotReplacementSetup;

/**
 * Dev mode hot replacement integration for server endpoints.
 * <p>
 * An application whose clients only communicate over open WebSocket connections never makes another HTTP request,
 * so the scan for source changes triggered by HTTP requests never runs. Messages received by server endpoints trigger
 * that scan instead, rate-limited to at most once every two seconds. When the application restarts, the open
 * connections are closed like any other connection of the previous deployment, and clients reconnect to the updated
 * application.
 */
public class WebSocketHotReplacementSetup implements HotReplacementSetup {

    private static final Logger LOG = Logger.getLogger(WebSocketHotReplacementSetup.class);

    private static final long TWO_SECONDS = 2000;

    private HotReplacementContext context;
    private volatile long nextUpdate;

    @Override
    public void setupHotDeployment(HotReplacementContext context) {
        this.context = context;
        WebSocketHotReplacementInterceptor.register(new ScanAction());
    }

    @Override
    public void close() {
        WebSocketHotReplacementInterceptor.shutdown();
    }

    private class ScanAction implements Supplier<Boolean> {

        @Override
        public Boolean get() {
            boolean restarted = false;
            synchronized (this) {
                if (nextUpdate < System.currentTimeMillis() || context.isTest()) {
                    try {
                        restarted = context.doScan(true);
                        if (context.getDeploymentProblem() != null) {
                            LOG.error("Failed to redeploy application on changes", context.getDeploymentProblem());
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
