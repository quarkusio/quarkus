package io.quarkus.arc.test.config.nativebuild;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.arc.config.NativeBuildTime;

@Singleton
public class Ok {

    @NativeBuildTime
    @ConfigProperty(name = "foo", defaultValue = "bar")
    String value;

    // triggers init during static init of native build
    void init(@Observes @Initialized(ApplicationScoped.class) Object event) {
    }
}
