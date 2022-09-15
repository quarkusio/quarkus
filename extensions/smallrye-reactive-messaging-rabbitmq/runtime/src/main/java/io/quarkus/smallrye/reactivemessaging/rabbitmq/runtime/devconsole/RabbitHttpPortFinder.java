package io.quarkus.smallrye.reactivemessaging.rabbitmq.runtime.devconsole;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.Config;

import io.quarkus.runtime.StartupEvent;

@Singleton
public class RabbitHttpPortFinder {

    String httpPort;

    void collect(@Observes StartupEvent event, Config config) {
        httpPort = config.getOptionalValue("rabbitmq-http-port", String.class).orElse(null);
    }
}
