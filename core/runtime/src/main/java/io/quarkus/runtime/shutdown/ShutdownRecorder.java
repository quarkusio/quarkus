package io.quarkus.runtime.shutdown;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ShutdownRecorder {

    private static final Logger log = Logger.getLogger(ShutdownRecorder.class);

    private static volatile List<ShutdownListener> shutdownListeners;
    private static volatile RuntimeValue<ShutdownConfig> shutdownConfig;
    private static volatile boolean delayEnabled;

    /**
     * ShutdownRecorder is only called in <code>RUNTIME_INIT</code> build steps, so it is safe to set ShutdownConfig.
     */
    public ShutdownRecorder(RuntimeValue<ShutdownConfig> shutdownConfig) {
        ShutdownRecorder.shutdownConfig = shutdownConfig;
    }

    public void setListeners(List<ShutdownListener> listeners, boolean delayEnabled) {
        shutdownListeners = Optional.ofNullable(listeners).orElseGet(List::of);
        ShutdownRecorder.delayEnabled = delayEnabled;
    }

    public static void runShutdown() {
        if (shutdownListeners == null) { // when QUARKUS_INIT_AND_EXIT is used, ShutdownRecorder#setListeners has not been called
            return;
        }
        log.debug("Attempting to gracefully shutdown.");
        try {
            executePreShutdown();
            waitForDelay();
            executeShutdown();
        } catch (Throwable e) {
            log.error("Graceful shutdown failed", e);
        }
    }

    private static void executePreShutdown() throws InterruptedException {
        CountDownLatch preShutdown = new CountDownLatch(shutdownListeners.size());
        for (ShutdownListener i : shutdownListeners) {
            i.preShutdown(new LatchShutdownNotification(preShutdown));
        }
        preShutdown.await();
    }

    private static void waitForDelay() {
        if (delayEnabled && shutdownConfig.getValue().isDelayEnabled()) {
            try {
                Thread.sleep(shutdownConfig.getValue().delay().get().toMillis());
            } catch (InterruptedException e) {
                log.error("Interrupted while waiting for delay, continuing to shutdown immediately");
            }
        }
    }

    private static void executeShutdown() throws InterruptedException {
        CountDownLatch shutdown = new CountDownLatch(shutdownListeners.size());
        for (ShutdownListener i : shutdownListeners) {
            i.shutdown(new LatchShutdownNotification(shutdown));
        }
        if (shutdownConfig.getValue().isTimeoutEnabled()
                && !shutdown.await(shutdownConfig.getValue().timeout().get().toMillis(), TimeUnit.MILLISECONDS)) {
            log.error("Timed out waiting for graceful shutdown, shutting down anyway.");
        }
    }

    private static class LatchShutdownNotification implements ShutdownListener.ShutdownNotification {
        private final CountDownLatch latch;

        public LatchShutdownNotification(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void done() {
            latch.countDown();
        }
    }
}
