package io.quarkus.csrf.reactive.runtime;

import java.util.function.Supplier;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class CsrfRecorder {

    public Supplier<CsrfReactiveConfig> configure(CsrfReactiveConfig csrfReactiveConfig) {
        return new Supplier<CsrfReactiveConfig>() {
            @Override
            public CsrfReactiveConfig get() {
                return csrfReactiveConfig;
            }
        };
    }

}
