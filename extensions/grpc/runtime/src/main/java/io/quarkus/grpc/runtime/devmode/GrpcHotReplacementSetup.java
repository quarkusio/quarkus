package io.quarkus.grpc.runtime.devmode;

import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.dev.spi.HotReplacementContext;
import io.quarkus.dev.spi.HotReplacementSetup;

public class GrpcHotReplacementSetup implements HotReplacementSetup {
    private static final Logger LOGGER = Logger.getLogger(GrpcHotReplacementSetup.class.getName());

    private HotReplacementContext context;
    private static final long TWO_SECONDS = 2000;

    private volatile long nextUpdate;

    @Override
    public void setupHotDeployment(HotReplacementContext context) {
        this.context = context;
        GrpcHotReplacementInterceptor.register(new RestartHandler());
    }

    @Override
    public void handleFailedInitialStart() {
        close();
    }

    @Override
    public void close() {
        GrpcServerReloader.shutdown();
    }

    private class RestartHandler implements Supplier<Boolean> {
        public Boolean get() {
            boolean restarted = false;
            synchronized (this) {
                if (nextUpdate < System.currentTimeMillis() || context.isTest()) {
                    try {
                        restarted = context.doScan(true);
                        if (context.getDeploymentProblem() != null) {
                            LOGGER.error("Failed to redeploy application on changes", context.getDeploymentProblem());
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
