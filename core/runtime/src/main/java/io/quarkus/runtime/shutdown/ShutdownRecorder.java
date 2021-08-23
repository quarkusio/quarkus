package io.quarkus.runtime.shutdown;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ShutdownRecorder {

    private static final Logger log = Logger.getLogger(ShutdownRecorder.class);
    public static final String WAITING_FOR_GRACEFUL_SHUTDOWN_SHUTTING_DOWN_ANYWAY = "Timed out waiting for graceful shutdown, shutting down anyway.";

    private static volatile List<ShutdownListener> shutdownListeners;
    private static volatile Optional<Duration> waitTime;

    public void setListeners(List<ShutdownListener> listeners, ShutdownConfig shutdownConfig) {
        shutdownListeners = listeners;
        waitTime = shutdownConfig.timeout;
    }

    public static void runShutdown() {
        try {
            CountDownLatch preShutdown = new CountDownLatch(shutdownListeners.size());
            for (ShutdownListener i : shutdownListeners) {
                i.preShutdown(new LatchShutdownNotification(preShutdown));
            }

            preShutdown.await();
            CountDownLatch shutdown = new CountDownLatch(shutdownListeners.size());
            for (ShutdownListener i : shutdownListeners) {
                i.shutdown(new LatchShutdownNotification(shutdown));
            }
            if (waitTime.isPresent()) {
                if (!shutdown.await(waitTime.get().toMillis(), TimeUnit.MILLISECONDS)) {
                    log.error(WAITING_FOR_GRACEFUL_SHUTDOWN_SHUTTING_DOWN_ANYWAY);
                }
            }

        } catch (Throwable e) {
            log.error("Graceful shutdown failed", e);
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
