package io.quarkus.arc.runtime.appcds;

import io.quarkus.runtime.PreventFurtherStepsException;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.init.InitializationTaskRecorder;

@Recorder
public class AppCDSRecorder {

    public static final String QUARKUS_APPCDS_GENERATE_PROP = "quarkus.appcds.generate";

    public void controlGenerationAndExit() {
        if (Boolean.parseBoolean(System.getProperty(QUARKUS_APPCDS_GENERATE_PROP, "false"))) {
            InitializationTaskRecorder.preventFurtherRecorderSteps(5,
                    "Unable to properly shutdown Quarkus application when creating AppCDS",
                    PreventFurtherStepsException::new);
        }
    }
}
