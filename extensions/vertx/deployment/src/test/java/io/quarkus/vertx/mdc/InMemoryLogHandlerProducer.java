package io.quarkus.vertx.mdc;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

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
