package io.quarkus.arc.runtime.appcds;

import io.quarkus.runtime.PreventFurtherStepsException;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.init.InitializationTaskRecorder;

@Recorder
public class AppCDSRecorder {

    public void controlGenerationAndExit() {
        if (Boolean.parseBoolean(System.getProperty("quarkus.appcds.generate", "false"))) {
            InitializationTaskRecorder.preventFurtherRecorderSteps(5,
                    "Unable to properly shutdown Quarkus application when creating AppCDS",
                    PreventFurtherStepsException::new);
        }
    }
}
