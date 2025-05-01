package io.quarkus.arc.runtime.appcds;

import io.quarkus.runtime.ApplicationLifecycleManager;
import io.quarkus.runtime.PreventFurtherStepsException;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.init.InitializationTaskRecorder;

@Recorder
public class JvmStartupOptimizerArchiveRecorder {

    public void controlGenerationAndExit() {
        if (ApplicationLifecycleManager.isAppCDSGeneration()) {
            InitializationTaskRecorder.preventFurtherRecorderSteps(5,
                    "Unable to properly shutdown Quarkus application when creating JVM startup archive",
                    PreventFurtherStepsException::new);
        }
    }
}
