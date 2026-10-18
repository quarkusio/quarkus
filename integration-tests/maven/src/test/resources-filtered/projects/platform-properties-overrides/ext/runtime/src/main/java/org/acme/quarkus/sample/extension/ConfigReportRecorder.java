package org.acme.quarkus.sample.extension;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ConfigReportRecorder {
	public RuntimeValue<ConfigReport> configReport(String builderImage) {
		return new RuntimeValue<>(new ConfigReport(builderImage));
    }
}
