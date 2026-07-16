package io.quarkus.devui.runtime;

import java.util.Optional;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkus.arc.DefaultBean;
import io.quarkus.assistant.runtime.dev.Assistant;

@Singleton
public class DefaultAssistantProducer {

    @Produces
    @DefaultBean
    public Optional<Assistant> defaultAssistant() {
        return Optional.empty();
    }
}
