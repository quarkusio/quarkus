package io.quarkus.qute.debug;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import io.quarkus.qute.EngineBuilder;
import io.quarkus.qute.debug.adapter.RegisterDebugServerAdapter;
import io.quarkus.runtime.LaunchMode;

@Singleton
public class DebugQuteEngineObserver {

    private final static RegisterDebugServerAdapter registrar = new RegisterDebugServerAdapter();

    void configureEngine(@Observes EngineBuilder builder) {
        if (LaunchMode.current() == LaunchMode.DEVELOPMENT) {
            builder.enableTracing(true);
            builder.addEngineListener(registrar);
        }
    }

    @PreDestroy
    void cleanup() {
        if (LaunchMode.current() == LaunchMode.DEVELOPMENT) {
            registrar.reset();
        }
    }
}
