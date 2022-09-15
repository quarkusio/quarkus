package io.quarkus.vertx.mdc;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkus.bootstrap.logging.InitialConfigurator;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class InMemoryLogHandlerProducer {

    @Produces
    @Singleton
    public InMemoryLogHandler inMemoryLogHandler() {
        return new InMemoryLogHandler();
    }

    void onStart(@Observes StartupEvent ev, InMemoryLogHandler inMemoryLogHandler) {
        InitialConfigurator.DELAYED_HANDLER.addHandler(inMemoryLogHandler);
    }

    void onStop(@Observes ShutdownEvent ev, InMemoryLogHandler inMemoryLogHandler) {
        InitialConfigurator.DELAYED_HANDLER.removeHandler(inMemoryLogHandler);
    }
}
