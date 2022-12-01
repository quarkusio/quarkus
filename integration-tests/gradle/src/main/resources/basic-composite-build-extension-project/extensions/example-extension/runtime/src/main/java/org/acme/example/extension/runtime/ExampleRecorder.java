package org.acme.example.extension.runtime;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.jboss.logmanager.formatters.PatternFormatter;

import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;

@Recorder
public class ExampleRecorder {


	private final QuarkusExampleExtensionConfig config;

	public ExampleRecorder(QuarkusExampleExtensionConfig config){
		this.config=config;
	}

	public RuntimeValue<Boolean> create() {
		boolean enabled = config.enabled;

		return new RuntimeValue<>(enabled);

	}
}
