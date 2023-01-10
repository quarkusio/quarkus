package io.quarkus.runtime.init;

import org.jboss.logging.Logger;

import io.quarkus.runtime.PreventFurtherStepsException;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

/**
 * A {@link Recorder} that is used to check if the application should exit once all initialization tasks are completed.
 */
@Recorder
public class InitializationTaskRecorder {

    private static final Logger log = Logger.getLogger(InitializationTaskRecorder.class);

    private final RuntimeValue<InitRuntimeConfig> config;

    public InitializationTaskRecorder(RuntimeValue<InitRuntimeConfig> config) {
        this.config = config;
    }

    public void exitIfNeeded() {
        if (config.getValue().initAndExit) {
            throw new PreventFurtherStepsException("Gracefully exiting after initalization.", 0);
        }
    }
}
