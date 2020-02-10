package io.quarkus.it.bootstrap.config.extension;

import java.util.Collections;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class DummyBootstrapRecorder2 {

    public RuntimeValue<ConfigSourceProvider> create() {
        return new RuntimeValue<>(new ConfigSourceProvider() {
            @Override
            public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
                return Collections.emptyList();
            }
        });
    }
}
