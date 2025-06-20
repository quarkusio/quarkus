package io.quarkus.runtime.init;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import io.quarkus.runtime.PreventFurtherStepsException;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

/**
 * A {@link Recorder} that is used to check if the application should exit once all initialization tasks are completed.
 */
@Recorder
public class InitializationTaskRecorder {
    private final RuntimeValue<InitRuntimeConfig> initRuntimeConfig;

    public InitializationTaskRecorder(RuntimeValue<InitRuntimeConfig> initRuntimeConfig) {
        this.initRuntimeConfig = initRuntimeConfig;
    }

    public void exitIfNeeded() {
        if (initRuntimeConfig.getValue().initAndExit()) {
            preventFurtherRecorderSteps(5, "Error attempting to gracefully shutdown after initialization",
                    () -> new PreventFurtherStepsException("Gracefully exiting after initialization.", 0));
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
