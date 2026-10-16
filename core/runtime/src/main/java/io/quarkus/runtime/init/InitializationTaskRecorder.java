package io.quarkus.runtime.init;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import io.quarkus.runtime.PreventFurtherStepsException;
import io.quarkus.runtime.Quarkus;

/**
 * Utility for checking if the application should exit once all initialization tasks are completed.
 */
public class InitializationTaskRecorder {

    private InitializationTaskRecorder() {
    }

    /**
     * Exit the application if init-and-exit mode is enabled.
     *
     * @param config the init runtime configuration
     */
    public static void exitIfNeeded(InitRuntimeConfig config) {
        if (config.initAndExit()) {
            preventFurtherRecorderSteps(5, "Error attempting to gracefully shutdown after initialization",
                    () -> new PreventFurtherStepsException("Gracefully exiting after initialization.", 0));
        }
    }

    /**
     * Initiate a graceful shutdown and throw a {@link PreventFurtherStepsException} to halt
     * remaining recorder steps.
     *
     * @param waitSeconds the maximum time to wait for shutdown
     * @param waitErrorMessage the message to log if interrupted
     * @param supplier the exception supplier
     */
    public static void preventFurtherRecorderSteps(int waitSeconds, String waitErrorMessage,
            Supplier<PreventFurtherStepsException> supplier) {
        CountDownLatch latch = new CountDownLatch(1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Quarkus.blockingExit();
                latch.countDown();
            }
        }).start();
        try {
            latch.await(waitSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.err.println(waitErrorMessage);
        }
        throw supplier.get();
    }
}
