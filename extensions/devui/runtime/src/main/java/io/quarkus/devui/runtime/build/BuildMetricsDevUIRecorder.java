package io.quarkus.devui.runtime.build;

import java.nio.file.Path;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class BuildMetricsDevUIRecorder {

    public void setBuildMetricsPath(String buildMetricsPath) {
        BuildMetricsDevUIController.get().setBuildMetricsPath(Path.of(buildMetricsPath));
    }
}
