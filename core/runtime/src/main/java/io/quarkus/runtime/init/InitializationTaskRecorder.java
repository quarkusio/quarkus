package io.quarkus.runtime.init;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.runtime.PreventFurtherStepsException;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.Recorder;

/**
 * A {@link Recorder} that is used to check if the application should exit once all initialization tasks are completed.
 */
@Recorder
public class InitializationTaskRecorder {

    private static final String QUARKUS_INIT_AND_EXIT = "quarkus.init-and-exit";

    public void exitIfNeeded() {
        //The tcks CustomConverTest is broken: It always returns true for boolean values.
        //To workaround this issue, we need to check if `init-and-exit` is explicitly defined.
        boolean initAndExitConfigured = StreamSupport.stream(ConfigProvider.getConfig().getPropertyNames().spliterator(), false)
                .anyMatch(n -> QUARKUS_INIT_AND_EXIT.equals(n));
        if (initAndExitConfigured) {
            if (ConfigProvider.getConfig().getValue(QUARKUS_INIT_AND_EXIT, boolean.class)) {
                preventFurtherRecorderSteps(5, "Error attempting to gracefully shutdown after initialization",
                        () -> new PreventFurtherStepsException("Gracefully exiting after initialization.", 0));
            }
        }
    }

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
