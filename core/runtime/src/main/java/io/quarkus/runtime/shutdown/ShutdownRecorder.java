package io.quarkus.runtime.shutdown;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ShutdownRecorder {

    private static final Logger log = Logger.getLogger(ShutdownRecorder.class);

    private static volatile List<ShutdownListener> shutdownListeners;
    private static volatile ShutdownConfig shutdownConfig;
    private static volatile boolean delayEnabled;

    public ShutdownRecorder(ShutdownConfig shutdownConfig) {
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
        if (delayEnabled && shutdownConfig.isDelaySet()) {
            try {
                Thread.sleep(shutdownConfig.delay.get().toMillis());
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
        if (shutdownConfig.isShutdownTimeoutSet()
                && !shutdown.await(shutdownConfig.timeout.get().toMillis(), TimeUnit.MILLISECONDS)) {
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
