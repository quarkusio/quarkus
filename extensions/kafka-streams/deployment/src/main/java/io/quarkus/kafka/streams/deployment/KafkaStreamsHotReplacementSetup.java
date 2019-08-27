package io.quarkus.kafka.streams.deployment;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.quarkus.deployment.devmode.HotReplacementContext;
import io.quarkus.deployment.devmode.HotReplacementSetup;
import io.quarkus.kafka.streams.runtime.HotReplacementInterceptor;

public class KafkaStreamsHotReplacementSetup implements HotReplacementSetup {

    private static final long TWO_SECONDS = 2000;

    private HotReplacementContext context;
    private long nextUpdate;
    private final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    public void setupHotDeployment(HotReplacementContext context) {
        this.context = context;
        HotReplacementInterceptor.onMessage(new OnMessage());
    }

    private class OnMessage implements Runnable {

        @Override
        public void run() {
            if (nextUpdate < System.currentTimeMillis()) {
                synchronized (this) {
                    if (nextUpdate < System.currentTimeMillis()) {
                        executor.execute(() -> {
                            try {
                                context.doScan(true);
                            } catch (RuntimeException e) {
                                throw e;
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                        // we update at most once every 2s
                        nextUpdate = System.currentTimeMillis() + TWO_SECONDS;
                    }
                }
            }
        }
    }
}
