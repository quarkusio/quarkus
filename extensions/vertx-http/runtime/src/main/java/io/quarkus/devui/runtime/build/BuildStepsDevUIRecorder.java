package io.quarkus.devui.runtime.build;

import java.nio.file.Path;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class BuildStepsDevUIRecorder {

    public void setBuildMetricsPath(String buildMetricsPath) {
        BuildStepsDevUIController.get().setBuildMetricsPath(Path.of(buildMetricsPath));
    }
}
