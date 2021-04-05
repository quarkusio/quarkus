package io.quarkus.smallrye.reactivemessaging.runtime.devmode;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.dev.spi.HotReplacementContext;
import io.quarkus.dev.spi.HotReplacementSetup;

public class ReactiveMessagingHotReplacementSetup implements HotReplacementSetup {
    private static final Logger LOGGER = Logger.getLogger(ReactiveMessagingHotReplacementSetup.class.getName());

    private static final long TWO_SECONDS = 2000;

    private HotReplacementContext context;
    private volatile long nextUpdate;
    private final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    public void setupHotDeployment(HotReplacementContext context) {
        this.context = context;
        DevModeSupportConnectorFactoryInterceptor.register(new OnMessage());
    }

    private class OnMessage implements Supplier<CompletableFuture<Boolean>> {
        @Override
        public CompletableFuture<Boolean> get() {
            if (nextUpdate < System.currentTimeMillis()) {
                synchronized (this) {
                    if (nextUpdate < System.currentTimeMillis()) {
                        CompletableFuture<Boolean> result = new CompletableFuture<>();
                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    boolean restarted = context.doScan(true);
                                    if (context.getDeploymentProblem() != null) {
                                        LOGGER.error("Failed to redeploy application on changes",
                                                context.getDeploymentProblem());
                                    }
                                    result.complete(restarted);
                                } catch (RuntimeException e) {
                                    throw e;
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                } finally {
                                    result.complete(false);
                                }
                            }
                        });
                        // we update at most once every 2s
                        nextUpdate = System.currentTimeMillis() + TWO_SECONDS;
                        return result;
                    }
                }
            }
            return CompletableFuture.completedFuture(false);
        }
    }
}
