package io.quarkus.arc.runtime.appcds;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.quarkus.runtime.PreventFurtherStepsException;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class AppCDSRecorder {

    public void controlGenerationAndExit() {
        if (Boolean.parseBoolean(System.getProperty("quarkus.appcds.generate", "false"))) {
            CountDownLatch latch = new CountDownLatch(1);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Quarkus.blockingExit();
                    latch.countDown();
                }
            }).start();
            try {
                latch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                System.err.println("Unable to properly shutdown Quarkus application when creating AppCDS");
            }
            throw new PreventFurtherStepsException();
        }
    }
}
