package io.quarkus.runtime;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class BootstrapConfigRecorder {

    public void run(RuntimeValue<StartupTask> runnable, StartupContext context) {
        runnable.getValue().deploy(context);
    }

    public void run(RuntimeValue<Runnable> runnable) {
        runnable.getValue().run();
    }
}
